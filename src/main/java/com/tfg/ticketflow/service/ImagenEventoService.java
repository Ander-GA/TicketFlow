
package com.tfg.ticketflow.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import com.tfg.ticketflow.entity.Evento;
import com.tfg.ticketflow.entity.Usuario;
import com.tfg.ticketflow.exception.ConflictoNegocioException;
import com.tfg.ticketflow.exception.RecursoNoEncontradoException;
import com.tfg.ticketflow.repository.EventoRepository;

@Service
public class ImagenEventoService {

    private static final Logger log =
            LoggerFactory.getLogger(ImagenEventoService.class);

    private static final long TAMANO_MAXIMO = 5L * 1024 * 1024;
    private static final long PIXELES_MAXIMOS = 12_000_000L;

    private static final String PREFIJO_URL =
            "/api/v1/imagenes/eventos/";

    private static final String PATRON_NOMBRE =
            "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-"
            + "[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png)";

    private final EventoRepository eventoRepository;
    private final Path directorio;

    public ImagenEventoService(
            EventoRepository eventoRepository,
            @Value("${ticketflow.imagenes.directorio:./uploads/eventos}")
            String rutaDirectorio) {

        this.eventoRepository = eventoRepository;
        this.directorio = Path.of(rutaDirectorio)
                .toAbsolutePath()
                .normalize();
    }

    // Subir o sustituir la imagen de un evento
    @Transactional
    public Evento subirImagen(Long id, Usuario organizador,
            MultipartFile archivo) {

        // Comprobar que el evento pertenece al organizador
        Evento evento = eventoRepository.buscarParaModificar(id, organizador)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se ha encontrado el evento o no pertenece al organizador."));

        // No permitir modificar eventos cancelados o finalizados
        if (evento.getEstado() == Evento.EstadoEvento.CANCELADO
                || evento.getEstado() == Evento.EstadoEvento.FINALIZADO) {
            throw new ConflictoNegocioException(
                    "No se puede modificar la imagen de este evento.");
        }

        // Validar la imagen y obtener una versión normalizada
        ImagenProcesada imagen = procesarImagen(archivo);

        // Generar un identificador independiente del nombre original
        String nombre = UUID.randomUUID() + imagen.extension();
        Path destino = directorio.resolve(nombre);

        try {
            Files.createDirectories(directorio);

            Files.write(
                    destino,
                    imagen.datos(),
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE
            );

        } catch (IOException ex) {
            eliminarArchivo(destino);
            throw new UncheckedIOException(
                    "No se ha podido almacenar la imagen.", ex);
        }

        String imagenAnterior = evento.getImagen();

        // Coordinar la limpieza de archivos con el resultado de MySQL
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {

                    @Override
                    public void afterCommit() {
                        // Borrar la imagen anterior solo tras confirmar MySQL
                        eliminarImagenAnterior(imagenAnterior);
                    }

                    @Override
                    public void afterCompletion(int estado) {
                        // Si la transacción falla, eliminar el archivo nuevo
                        if (estado != STATUS_COMMITTED) {
                            eliminarArchivo(destino);
                        }
                    }
                }
        );

        // Guardar una URL estable gestionada por nuestra API
        evento.setImagen(PREFIJO_URL + nombre);

        return eventoRepository.save(evento);
    }

    // Obtener una imagen almacenada
    public Resource obtenerImagen(String nombre) {

        // Impedir acceder a rutas arbitrarias del sistema de archivos
        if (nombre == null || !nombre.matches(PATRON_NOMBRE)) {
            throw new RecursoNoEncontradoException(
                    "No se ha encontrado la imagen.");
        }

        Path archivo = directorio.resolve(nombre).normalize();

        if (!archivo.startsWith(directorio)
                || !Files.isRegularFile(archivo)
                || !Files.isReadable(archivo)) {
            throw new RecursoNoEncontradoException(
                    "No se ha encontrado la imagen.");
        }

        return new FileSystemResource(archivo);
    }

    // Comprobar el contenido real y normalizar la imagen
    private ImagenProcesada procesarImagen(MultipartFile archivo) {

        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException(
                    "Debes seleccionar una imagen.");
        }

        if (archivo.getSize() > TAMANO_MAXIMO) {
            throw new IllegalArgumentException(
                    "La imagen no puede superar los 5 MB.");
        }

        try (ImageInputStream entrada = ImageIO.createImageInputStream(
                archivo.getInputStream())) {

            if (entrada == null) {
                throw new IllegalArgumentException(
                        "El archivo no es una imagen válida.");
            }

            Iterator<ImageReader> lectores =
                    ImageIO.getImageReaders(entrada);

            if (!lectores.hasNext()) {
                throw new IllegalArgumentException(
                        "El archivo no es una imagen válida.");
            }

            ImageReader lector = lectores.next();

            try {
                lector.setInput(entrada);

                String formato = lector.getFormatName()
                        .toLowerCase(Locale.ROOT);

                String extension;

                if (formato.equals("jpeg") || formato.equals("jpg")) {
                    extension = ".jpg";

                } else if (formato.equals("png")) {
                    extension = ".png";

                } else {
                    throw new IllegalArgumentException(
                            "Solo se permiten imágenes JPG y PNG.");
                }

                // Limitar dimensiones antes de decodificar
                int ancho = lector.getWidth(0);
                int alto = lector.getHeight(0);

                if (ancho <= 0 || alto <= 0
                        || ancho > 4000 || alto > 4000
                        || (long) ancho * alto > PIXELES_MAXIMOS) {
                    throw new IllegalArgumentException(
                            "La imagen supera las dimensiones permitidas.");
                }

                BufferedImage imagen = lector.read(0);

                // Volver a codificar para no conservar el archivo original
                ByteArrayOutputStream salida =
                        new ByteArrayOutputStream();

                String formatoSalida = extension.equals(".jpg")
                        ? "jpeg" : "png";

                if (!ImageIO.write(imagen, formatoSalida, salida)) {
                    throw new IllegalArgumentException(
                            "No se ha podido procesar la imagen.");
                }

                if (salida.size() > TAMANO_MAXIMO) {
                    throw new IllegalArgumentException(
                            "La imagen procesada supera los 5 MB.");
                }

                return new ImagenProcesada(
                        salida.toByteArray(), extension);

            } finally {
                lector.dispose();
            }

        } catch (IOException ex) {
            throw new IllegalArgumentException(
                    "No se ha podido leer la imagen.", ex);
        }
    }

    // Eliminar una imagen anterior gestionada por TicketFlow
    private void eliminarImagenAnterior(String referencia) {

        if (referencia == null
                || !referencia.startsWith(PREFIJO_URL)) {
            return;
        }

        String nombre = referencia.substring(PREFIJO_URL.length());

        if (!nombre.matches(PATRON_NOMBRE)) {
            return;
        }

        eliminarArchivo(directorio.resolve(nombre));
    }

    // Eliminar un archivo y registrar los posibles fallos
    private void eliminarArchivo(Path archivo) {

        try {
            Files.deleteIfExists(archivo);

        } catch (IOException ex) {
            log.warn(
                    "No se ha podido eliminar el archivo: {}",
                    archivo, ex);
        }
    }

    // Datos de la imagen validada
    private record ImagenProcesada(byte[] datos, String extension) {
    }
}
