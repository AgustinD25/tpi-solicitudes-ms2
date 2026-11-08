package com.tup.solicitudes;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Casos de uso sobre solicitudes. Aplica las reglas del dominio y las informa con excepciones;
 * no imprime nada ni conoce como se guardan los datos (depende de las interfaces de repositorio).
 */
public class SolicitudService {
    static final int MAX_PENDIENTES_POR_ESTUDIANTE = 3;
    static final int PRIORIDAD_MINIMA = 1;
    static final int PRIORIDAD_MAXIMA = 3;

    private final SolicitudRepository solicitudRepository;
    private final EstudianteRepository estudianteRepository;
    private final CatalogoResponsables responsableService;

    public SolicitudService(SolicitudRepository solicitudRepository, EstudianteRepository estudianteRepository,
                            CatalogoResponsables responsableService) {
        this.solicitudRepository = solicitudRepository;
        this.estudianteRepository = estudianteRepository;
        this.responsableService = responsableService;
    }

    public Solicitud registrar(NuevaSolicitud datos) {
        validarDatosBasicos(datos);
        if (!ValidadorEmail.esValido(datos.email())) {
            throw new DatosInvalidosException("email invalido");
        }
        Estudiante estudianteIngresado = new Estudiante(datos.legajo(), datos.nombre(), datos.email());
        if (contarPendientes(datos.legajo()) >= MAX_PENDIENTES_POR_ESTUDIANTE) {
            throw new LimitePendientesException(MAX_PENDIENTES_POR_ESTUDIANTE);
        }
        Estudiante estudiante = buscarOCrearEstudiante(estudianteIngresado);
        int prioridadFinal = calcularPrioridad(datos.prioridad(), datos.urgente());
        Solicitud nueva = new Solicitud(solicitudRepository.generarId(), estudiante,
                TipoSolicitud.desde(datos.tipo()).orElseThrow(TipoInvalidoException::new), datos.descripcion(), prioridadFinal, datos.urgente(),
                datos.fecha());
        solicitudRepository.guardar(nueva);
        return nueva;
    }

    public Solicitud cambiarEstado(int id, String nuevoEstado) {
        Solicitud solicitud = buscarPorId(id);
        EstadoSolicitud destino = EstadoSolicitud.desde(nuevoEstado)
                .filter(solicitud::puedePasarA)
                .orElseThrow(() -> new TransicionInvalidaException(solicitud.getEstado(), nuevoEstado));
        solicitud.pasarA(destino);
        solicitudRepository.actualizar(solicitud);
        return solicitud;
    }

    public Solicitud asignarResponsable(int idSolicitud, int idResponsable) {
        Solicitud solicitud = buscarPorId(idSolicitud);
        Responsable responsable = responsableService.buscarPorId(idResponsable)
                .orElseThrow(() -> new ResponsableNoEncontradoException(idResponsable));
        solicitud.asignarResponsable(responsable);   // la entidad valida que este abierta y que el area coincida
        solicitudRepository.actualizar(solicitud);
        return solicitud;
    }

    public List<Solicitud> filtrar(Predicate<Solicitud> criterio) {
        List<Solicitud> resultado = new ArrayList<>();
        for (Solicitud solicitud : solicitudRepository.listarTodas()) {
            if (criterio.test(solicitud)) {
                resultado.add(solicitud);
            }
        }
        return resultado;
    }

    public List<Solicitud> listarTodas() {
        return solicitudRepository.listarTodas();
    }

    public List<Estudiante> listarEstudiantes() {
        return estudianteRepository.listarTodos();
    }

    /** Incorpora lo guardado en el almacenamiento persistente (no hace nada si es solo en memoria). */
    public void cargar() {
        solicitudRepository.cargar();
    }

    private Solicitud buscarPorId(int id) {
        return solicitudRepository.buscarPorId(id).orElseThrow(() -> new SolicitudNoEncontradaException(id));
    }

    // Valida legajo, nombre, tipo y prioridad, en ese orden (el orden determina cual error se informa).
    private void validarDatosBasicos(NuevaSolicitud datos) {
        if (datos.legajo() == null || datos.legajo().trim().length() == 0) {
            throw new DatosInvalidosException("legajo vacio");
        }
        if (datos.nombre() == null || datos.nombre().trim().length() == 0) {
            throw new DatosInvalidosException("nombre vacio");
        }
        if (TipoSolicitud.desde(datos.tipo()).isEmpty()) {
            throw new TipoInvalidoException();
        }
        if (datos.prioridad() < PRIORIDAD_MINIMA || datos.prioridad() > PRIORIDAD_MAXIMA) {
            throw new PrioridadInvalidaException();
        }
    }

    private int contarPendientes(String legajo) {
        return filtrar(s -> s.getEstudiante().getLegajo().equals(legajo)
                && s.getEstadoSolicitud() == EstadoSolicitud.PENDIENTE).size();
    }

    // Devuelve el estudiante ya registrado con ese legajo o, si no existe, registra el ingresado.
    private Estudiante buscarOCrearEstudiante(Estudiante estudianteIngresado) {
        Estudiante estudiante = estudianteRepository.buscarPorLegajo(estudianteIngresado.getLegajo()).orElse(null);
        if (estudiante == null) {
            estudiante = estudianteIngresado;
            estudianteRepository.agregar(estudiante);
        }
        return estudiante;
    }

    // Una solicitud urgente sube un nivel de prioridad, con tope en PRIORIDAD_MAXIMA.
    private int calcularPrioridad(int prioridad, boolean urgente) {
        if (!urgente) {
            return prioridad;
        }
        return Math.min(prioridad + 1, PRIORIDAD_MAXIMA);
    }
}
