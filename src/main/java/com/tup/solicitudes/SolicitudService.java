package com.tup.solicitudes;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
    private final ResponsableService responsableService;

    public SolicitudService(SolicitudRepository solicitudRepository, EstudianteRepository estudianteRepository,
                            ResponsableService responsableService) {
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
                TipoSolicitud.desde(datos.tipo()), datos.descripcion(), prioridadFinal, datos.urgente(),
                datos.fecha());
        solicitudRepository.guardar(nueva);
        return nueva;
    }

    public Solicitud cambiarEstado(int id, String nuevoEstado) {
        Solicitud solicitud = buscarPorId(id);
        if (!solicitud.getEstadoSolicitud().esFinal()) {
            // Defecto D5 conservado a proposito: con estado nulo la version original lanzaba
            // NullPointerException (salvo si la solicitud ya estaba en un estado final).
            Objects.requireNonNull(nuevoEstado);
        }
        EstadoSolicitud destino = EstadoSolicitud.desde(nuevoEstado);
        if (destino == null || !solicitud.puedePasarA(destino)) {
            throw new TransicionInvalidaException(solicitud.getEstado(), nuevoEstado);
        }
        solicitud.pasarA(destino);
        solicitudRepository.actualizar(solicitud);
        return solicitud;
    }

    public Solicitud asignarResponsable(int idSolicitud, int idResponsable) {
        Solicitud solicitud = buscarPorId(idSolicitud);
        if (solicitud.getEstadoSolicitud().esFinal()) {
            throw new SolicitudCerradaException();
        }
        Responsable responsable = responsableService.buscarPorId(idResponsable)
                .orElseThrow(() -> new ResponsableNoEncontradoException(idResponsable));
        if (!solicitud.puedeSerAtendidaPor(responsable)) {
            throw new AreaIncorrectaException(responsable.getArea(),
                    solicitud.getTipoSolicitud().getAreaResponsable());
        }
        solicitud.asignarResponsable(responsable);
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
        if (TipoSolicitud.desde(datos.tipo()) == null) {
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
