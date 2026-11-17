#include <AFMotor.h>
#include <SoftwareSerial.h>

/*
 * Rover Control - firmware compatible con la app Android.
 *
 * Comandos (ASCII, terminados en '\n'):
 *   F/B/L/R  mover adelante/atras/izquierda/derecha
 *   S        detener y rearmar
 *   V0..255  cambiar velocidad (turbo = V255, brake = V0 + S)
 *   X        E-STOP: detiene y bloquea hasta recibir S
 *   W0/W1    luces opcionales (definir PIN_LUCES)
 *
 * La app envia un comando solo cuando cambia la direccion, por eso el
 * Arduino mantiene el movimiento hasta recibir otro comando.
 */

SoftwareSerial bluetoothSerial(9, 10); // RX, TX del HC-05/HC-06

AF_DCMotor motor1(1, MOTOR12_1KHZ);
AF_DCMotor motor2(2, MOTOR12_1KHZ);
AF_DCMotor motor3(3, MOTOR34_1KHZ);
AF_DCMotor motor4(4, MOTOR34_1KHZ);

int velocidad = 153; // 60 %, igual que el valor inicial de la app
bool parada = false;
char movimiento = 'S';

char bufferComando[16];
uint8_t longitudComando = 0;

// Si conectas un relay, MOSFET o LED, descomenta y ajusta el pin.
// #define PIN_LUCES 13

void detener() {
  motor1.setSpeed(0);
  motor1.run(RELEASE);
  motor2.setSpeed(0);
  motor2.run(RELEASE);
  motor3.setSpeed(0);
  motor3.run(RELEASE);
  motor4.setSpeed(0);
  motor4.run(RELEASE);
}

void mover(uint8_t m1, uint8_t m2, uint8_t m3, uint8_t m4) {
  motor1.setSpeed(velocidad);
  motor1.run(m1);
  motor2.setSpeed(velocidad);
  motor2.run(m2);
  motor3.setSpeed(velocidad);
  motor3.run(m3);
  motor4.setSpeed(velocidad);
  motor4.run(m4);
}

void aplicarMovimiento() {
  if (parada || velocidad == 0) {
    detener();
    return;
  }

  switch (movimiento) {
    case 'F': mover(FORWARD, FORWARD, FORWARD, FORWARD); break;
    case 'B': mover(BACKWARD, BACKWARD, BACKWARD, BACKWARD); break;
    case 'L': mover(FORWARD, BACKWARD, BACKWARD, FORWARD); break;
    case 'R': mover(BACKWARD, FORWARD, FORWARD, BACKWARD); break;
    default: detener(); break;
  }
}

void iniciarMovimiento(char nuevoMovimiento) {
  if (parada || velocidad == 0) {
    detener();
    return;
  }

  movimiento = nuevoMovimiento;
  aplicarMovimiento();
}

void procesarComando(char *comando) {
  char operacion = comando[0];

  if (operacion == 'V') {
    if (comando[1] < '0' || comando[1] > '9') {
      return;
    }

    velocidad = 0;
    for (char *p = comando + 1; *p >= '0' && *p <= '9'; p++) {
      velocidad = (velocidad * 10) + (*p - '0');
      if (velocidad > 255) {
        velocidad = 255;
      }
    }

    if (!parada) {
      if (velocidad == 0) {
        movimiento = 'S';
        detener();
      } else {
        aplicarMovimiento();
      }
    }
    return;
  }

  switch (operacion) {
    case 'F':
    case 'B':
    case 'L':
    case 'R':
      iniciarMovimiento(operacion);
      break;

    case 'S':
      parada = false;
      movimiento = 'S';
      detener();
      break;

    case 'X':
      parada = true;
      movimiento = 'S';
      detener();
      break;

    case 'W':
#ifdef PIN_LUCES
      digitalWrite(PIN_LUCES, comando[1] == '1' ? HIGH : LOW);
#endif
      break;
  }
}

void leerBluetooth() {
  while (bluetoothSerial.available() > 0) {
    char caracter = bluetoothSerial.read();

    if (caracter == '\n') {
      bufferComando[longitudComando] = '\0';
      if (longitudComando > 0) {
        procesarComando(bufferComando);
      }
      longitudComando = 0;
    } else if (caracter != '\r' && longitudComando < sizeof(bufferComando) - 1) {
      bufferComando[longitudComando++] = caracter;
    }
  }
}

void setup() {
  bluetoothSerial.begin(9600);

#ifdef PIN_LUCES
  pinMode(PIN_LUCES, OUTPUT);
  digitalWrite(PIN_LUCES, LOW);
#endif

  detener();
}

void loop() {
  leerBluetooth();
}
