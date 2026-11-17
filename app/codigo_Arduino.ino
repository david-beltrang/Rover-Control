#include <AFMotor.h>
#include <SoftwareSerial.h>

/*
 * Rover Control - firmware de compatibilidad con la app Android.
 *
 * Protocolo (ASCII, terminado en '\n'):
 *   F\n          avanzar
 *   B\n          retroceder
 *   L\n          girar a la izquierda
 *   R\n          girar a la derecha
 *   S\n          detener motores y salir de E-STOP / rearmar
 *   V<0-255>\n   ajustar velocidad (turbo usa V255 y brake usa V0 + S)
 *   X\n          E-STOP: detiene y bloquea el movimiento hasta recibir S
 *   W0\n/W1\n    luces (opcional, ver PIN_LUCES)
 *
 * La app envia los comandos de movimiento solo cuando cambia la direccion;
 * por eso el Arduino mantiene el movimiento hasta que llegue otro comando.
 */

SoftwareSerial bluetoothSerial(9, 10); // RX, TX del HC-05/HC-06

// Motores del shield AFMotor.
AF_DCMotor motor1(1, MOTOR12_1KHZ);
AF_DCMotor motor2(2, MOTOR12_1KHZ);
AF_DCMotor motor3(3, MOTOR34_1KHZ);
AF_DCMotor motor4(4, MOTOR34_1KHZ);

const uint8_t VELOCIDAD_MAXIMA = 255;
const uint8_t VELOCIDAD_INICIAL = 153; // 60 %, igual que el valor por defecto de la app
const uint8_t TAMANO_BUFFER = 16;

enum Movimiento {
  MOVIMIENTO_DETENIDO,
  MOVIMIENTO_ADELANTE,
  MOVIMIENTO_ATRAS,
  MOVIMIENTO_IZQUIERDA,
  MOVIMIENTO_DERECHA
};

Movimiento movimientoActual = MOVIMIENTO_DETENIDO;
uint8_t velocidadActual = VELOCIDAD_INICIAL;
bool paradaEmergencia = false;

char bufferComando[TAMANO_BUFFER];
uint8_t longitudComando = 0;
bool descartarLinea = false;

// Prototipos (el IDE de Arduino tambien los genera automaticamente).
void leerBluetooth();
void procesarComando(char *comando);
int leerVelocidad(const char *texto);
void iniciarMovimiento(Movimiento movimiento);
void aplicarMovimiento();
void detenerMovimiento();
void detenerMotores();

// Si conectas un relay, MOSFET o LED para las luces, descomenta y ajusta el pin.
// La app envia W1 para encender y W0 para apagar.
// #define PIN_LUCES 13

void setup() {
  bluetoothSerial.begin(9600);

#ifdef PIN_LUCES
  pinMode(PIN_LUCES, OUTPUT);
  digitalWrite(PIN_LUCES, LOW);
#endif

  detenerMotores();
}

void loop() {
  leerBluetooth();
}

void leerBluetooth() {
  while (bluetoothSerial.available() > 0) {
    char caracter = (char)bluetoothSerial.read();

    if (caracter == '\r') {
      continue;
    }

    if (caracter == '\n') {
      if (!descartarLinea && longitudComando > 0) {
        bufferComando[longitudComando] = '\0';
        procesarComando(bufferComando);
      }
      longitudComando = 0;
      descartarLinea = false;
      continue;
    }

    if (descartarLinea) {
      continue;
    }

    if (longitudComando < TAMANO_BUFFER - 1) {
      bufferComando[longitudComando++] = caracter;
    } else {
      // Linea demasiado larga: se ignora completa hasta el siguiente '\n'.
      longitudComando = 0;
      descartarLinea = true;
    }
  }
}

void procesarComando(char *comando) {
  while (*comando == ' ') {
    comando++;
  }

  char operacion = comando[0];
  if (operacion >= 'a' && operacion <= 'z') {
    operacion = operacion - 'a' + 'A';
  }

  switch (operacion) {
    case 'F':
      iniciarMovimiento(MOVIMIENTO_ADELANTE);
      break;

    case 'B':
      iniciarMovimiento(MOVIMIENTO_ATRAS);
      break;

    case 'L':
      iniciarMovimiento(MOVIMIENTO_IZQUIERDA);
      break;

    case 'R':
      iniciarMovimiento(MOVIMIENTO_DERECHA);
      break;

    case 'S':
      // S detiene y tambien rearma despues de un E-STOP.
      paradaEmergencia = false;
      detenerMovimiento();
      break;

    case 'V': {
      int valor = leerVelocidad(comando + 1);
      if (valor < 0) {
        break;
      }

      velocidadActual = (uint8_t)valor;

      if (paradaEmergencia) {
        // Mantiene la velocidad guardada, pero no mueve hasta recibir S.
        break;
      }

      if (velocidadActual == 0) {
        detenerMovimiento();
      } else {
        aplicarMovimiento();
      }
      break;
    }

    case 'X':
      // E-STOP con enclavamiento: solo S vuelve a habilitar el movimiento.
      paradaEmergencia = true;
      detenerMovimiento();
      break;

    case 'W':
#ifdef PIN_LUCES
      digitalWrite(PIN_LUCES, comando[1] == '1' ? HIGH : LOW);
#endif
      break;

    default:
      // Comando desconocido: se ignora para no detener el rover por ruido.
      break;
  }
}

int leerVelocidad(const char *texto) {
  while (*texto == ' ') {
    texto++;
  }

  long valor = 0;
  bool tieneDigito = false;

  while (*texto >= '0' && *texto <= '9') {
    tieneDigito = true;
    valor = (valor * 10) + (*texto - '0');
    if (valor > 1000) {
      valor = 1000;
    }
    texto++;
  }

  if (!tieneDigito) {
    return -1;
  }

  if (valor < 0) {
    valor = 0;
  }
  if (valor > VELOCIDAD_MAXIMA) {
    valor = VELOCIDAD_MAXIMA;
  }
  return (int)valor;
}

void iniciarMovimiento(Movimiento movimiento) {
  if (paradaEmergencia || velocidadActual == 0) {
    detenerMovimiento();
    return;
  }

  movimientoActual = movimiento;
  aplicarMovimiento();
}

void aplicarMovimiento() {
  if (paradaEmergencia || velocidadActual == 0 ||
      movimientoActual == MOVIMIENTO_DETENIDO) {
    detenerMotores();
    return;
  }

  switch (movimientoActual) {
    case MOVIMIENTO_ADELANTE:
      motor1.setSpeed(velocidadActual);
      motor1.run(FORWARD);
      motor2.setSpeed(velocidadActual);
      motor2.run(FORWARD);
      motor3.setSpeed(velocidadActual);
      motor3.run(FORWARD);
      motor4.setSpeed(velocidadActual);
      motor4.run(FORWARD);
      break;

    case MOVIMIENTO_ATRAS:
      motor1.setSpeed(velocidadActual);
      motor1.run(BACKWARD);
      motor2.setSpeed(velocidadActual);
      motor2.run(BACKWARD);
      motor3.setSpeed(velocidadActual);
      motor3.run(BACKWARD);
      motor4.setSpeed(velocidadActual);
      motor4.run(BACKWARD);
      break;

    case MOVIMIENTO_IZQUIERDA:
      motor1.setSpeed(velocidadActual);
      motor1.run(FORWARD);
      motor2.setSpeed(velocidadActual);
      motor2.run(BACKWARD);
      motor3.setSpeed(velocidadActual);
      motor3.run(BACKWARD);
      motor4.setSpeed(velocidadActual);
      motor4.run(FORWARD);
      break;

    case MOVIMIENTO_DERECHA:
      motor1.setSpeed(velocidadActual);
      motor1.run(BACKWARD);
      motor2.setSpeed(velocidadActual);
      motor2.run(FORWARD);
      motor3.setSpeed(velocidadActual);
      motor3.run(FORWARD);
      motor4.setSpeed(velocidadActual);
      motor4.run(BACKWARD);
      break;

    case MOVIMIENTO_DETENIDO:
    default:
      detenerMotores();
      break;
  }
}

void detenerMovimiento() {
  movimientoActual = MOVIMIENTO_DETENIDO;
  detenerMotores();
}

void detenerMotores() {
  motor1.setSpeed(0);
  motor1.run(RELEASE);
  motor2.setSpeed(0);
  motor2.run(RELEASE);
  motor3.setSpeed(0);
  motor3.run(RELEASE);
  motor4.setSpeed(0);
  motor4.run(RELEASE);
}
