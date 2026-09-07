package com.example.inventariotecnikor.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

/**
 * Genera la imagen (PNG) de un codigo QR a partir de un texto.
 *
 * Se usa para:
 *  - mostrar el QR en la ficha del producto (pantalla),
 *  - dibujarlo en la pagina de etiquetas que se manda a imprimir.
 *
 * La impresora Bixolon, cuando imprimamos "directo" (paso A3), dibuja el QR
 * ella misma con ZPL; esta clase es para todo lo demas.
 */
@Service
public class QrService {

    /**
     * @param contenido texto que codifica el QR (aqui, el codigoQr del producto)
     * @param tamanoPx  ancho = alto de la imagen resultante, en pixeles
     */
    public byte[] pngDeTexto(String contenido, int tamanoPx) {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 1); // "quiet zone" pequeña
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);

        try {
            BitMatrix matriz = new QRCodeWriter()
                    .encode(contenido, BarcodeFormat.QR_CODE, tamanoPx, tamanoPx, hints);

            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matriz, "PNG", salida);
            return salida.toByteArray();

        } catch (WriterException | IOException e) {
            throw new IllegalStateException("No se pudo generar el QR para: " + contenido, e);
        }
    }
}
