#include <AFMotor_R4.h> 
#include <SoftwareSerial.h> 
#include <Wire.h>
#include <Adafruit_GFX.h>
#include <Adafruit_SSD1306.h>

// Configuración de la Pantalla OLED
#define ANCHO_PANTALLA 128
#define ALTO_PANTALLA 64
#define OLED_RESET -1
Adafruit_SSD1306 display(ANCHO_PANTALLA, ALTO_PANTALLA, &Wire, OLED_RESET);

// Configuración Bluetooth y Motores
SoftwareSerial bluetoothSerial(9, 10); // RX, TX del HC-05/HC-06

AF_DCMotor motor1(1);
AF_DCMotor motor2(2);
AF_DCMotor motor3(3);
AF_DCMotor motor4(4);

// Configuración de los LEDs
const int PIN_LED_AMARILLO_1 = 5;
const int PIN_LED_AMARILLO_2 = 8;
const int PIN_LED_ROJO_1 = 6;
const int PIN_LED_ROJO_2 = 7;

// Buzzer (botón HONK de la app)
const uint8_t PIN_BUZZER = 13;
const unsigned int FRECUENCIA_BUZZER = 1200;

// Sensor Ultrasónico
#define PIN_TRIG 0
#define PIN_ECHO 1
long duracionUltrasonido;
int distancia = -1; // -1 = sin lectura todavía

int velocidad = 153; 
bool parada = false; 
char movimiento = 'S'; 

// Sensores
int pinTemperatura = A2; 
int a;                   
float temperatura;       
int B = 3975;            
float resistencia;       

const uint8_t PIN_SENSOR_LUZ = A3; 
int valLuz = 1023; // Luz máxima hasta la primera lectura real


// --- Umbrales de las condiciones automáticas ---
const int UMBRAL_DISTANCIA_SEGURIDAD = 20; // cm: parar si avanzamos con algo mas cerca
const int UMBRAL_LUZ_BAJA = 200;           // valLuz invertido: menos de 200 = oscuro
bool lucesAppEncendidas = false;           // Estado pedido por la app con W1/W0

// Variables de control
char bufferComando[16]; 
uint8_t longitudComando = 0; 

const unsigned long INTERVALO_TELEMETRIA = 500; 
unsigned long ultimaTelemetria = 0; 

char caraActual = ' '; 

// Funciones de la Pantalla (Caritas)

void caraNeutra() {
  display.clearDisplay();
  display.fillRect(28, 15, 20, 3, SSD1306_WHITE);
  display.fillRect(80, 15, 20, 3, SSD1306_WHITE);
  display.fillRect(30, 22, 24, 10, SSD1306_WHITE); 
  display.fillCircle(42, 32, 12, SSD1306_WHITE);   
  display.fillRect(74, 22, 24, 10, SSD1306_WHITE);
  display.fillCircle(86, 32, 12, SSD1306_WHITE);
  display.fillRect(38, 28, 8, 8, SSD1306_BLACK);   
  display.fillRect(40, 30, 4, 4, SSD1306_WHITE);   
  display.fillRect(82, 28, 8, 8, SSD1306_BLACK);
  display.fillRect(84, 30, 4, 4, SSD1306_WHITE);
  display.fillRect(54, 52, 20, 2, SSD1306_WHITE);
  display.display();
}

void caraEnojada() {
  display.clearDisplay();
  display.drawLine(25, 12, 50, 18, SSD1306_WHITE);
  display.drawLine(25, 13, 50, 19, SSD1306_WHITE); 
  display.drawLine(103, 12, 78, 18, SSD1306_WHITE);
  display.drawLine(103, 13, 78, 19, SSD1306_WHITE);
  display.fillRect(30, 22, 24, 10, SSD1306_WHITE); 
  display.fillCircle(42, 32, 12, SSD1306_WHITE);   
  display.fillRect(74, 22, 24, 10, SSD1306_WHITE);
  display.fillCircle(86, 32, 12, SSD1306_WHITE);
  display.fillRect(38, 28, 8, 8, SSD1306_BLACK);   
  display.fillRect(40, 30, 4, 4, SSD1306_WHITE);   
  display.fillRect(82, 28, 8, 8, SSD1306_BLACK);
  display.fillRect(84, 30, 4, 4, SSD1306_WHITE);
  display.fillRect(54, 52, 20, 2, SSD1306_WHITE);
  display.display();
}

void caraFeliz() {
  display.clearDisplay();
  display.fillRect(28, 15, 20, 3, SSD1306_WHITE);
  display.fillRect(80, 15, 20, 3, SSD1306_WHITE);
  display.fillRect(30, 22, 24, 10, SSD1306_WHITE); 
  display.fillCircle(42, 32, 12, SSD1306_WHITE);   
  display.fillRect(74, 22, 24, 10, SSD1306_WHITE);
  display.fillCircle(86, 32, 12, SSD1306_WHITE);
  display.fillRect(38, 30, 8, 8, SSD1306_BLACK);   
  display.fillRect(40, 32, 4, 4, SSD1306_WHITE);   
  display.fillRect(82, 30, 8, 8, SSD1306_BLACK);
  display.fillRect(84, 32, 4, 4, SSD1306_WHITE);
  display.fillCircle(64, 50, 16, SSD1306_WHITE); 
  display.fillRect(0, 0, 128, 50, SSD1306_BLACK); 
  display.fillRect(48, 50, 32, 2, SSD1306_WHITE); 
  display.display();
}

void caraSorprendidaIzquierda() {
  display.clearDisplay();
  display.drawLine(25, 19, 50, 12, SSD1306_WHITE);
  display.drawLine(25, 20, 50, 13, SSD1306_WHITE);
  display.drawLine(103, 19, 78, 12, SSD1306_WHITE);
  display.drawLine(103, 20, 78, 13, SSD1306_WHITE);
  display.fillRect(30, 22, 24, 10, SSD1306_WHITE); 
  display.fillCircle(42, 32, 12, SSD1306_WHITE);   
  display.fillRect(74, 22, 24, 10, SSD1306_WHITE);
  display.fillCircle(86, 32, 12, SSD1306_WHITE);
  display.fillRect(34, 28, 8, 8, SSD1306_BLACK);   
  display.fillRect(36, 30, 4, 4, SSD1306_WHITE);   
  display.fillRect(78, 28, 8, 8, SSD1306_BLACK);
  display.fillRect(80, 30, 4, 4, SSD1306_WHITE);
  display.fillCircle(64, 52, 7, SSD1306_WHITE);
  display.display();
}

void caraSorprendidaDerecha() {
  display.clearDisplay();
  display.drawLine(25, 19, 50, 12, SSD1306_WHITE);
  display.drawLine(25, 20, 50, 13, SSD1306_WHITE);
  display.drawLine(103, 19, 78, 12, SSD1306_WHITE);
  display.drawLine(103, 20, 78, 13, SSD1306_WHITE);
  display.fillRect(30, 22, 24, 10, SSD1306_WHITE); 
  display.fillCircle(42, 32, 12, SSD1306_WHITE);   
  display.fillRect(74, 22, 24, 10, SSD1306_WHITE);
  display.fillCircle(86, 32, 12, SSD1306_WHITE);
  display.fillRect(42, 28, 8, 8, SSD1306_BLACK);   
  display.fillRect(44, 30, 4, 4, SSD1306_WHITE);   
  display.fillRect(86, 28, 8, 8, SSD1306_BLACK);
  display.fillRect(88, 30, 4, 4, SSD1306_WHITE);
  display.fillCircle(64, 52, 7, SSD1306_WHITE);
  display.display();
}

// Funciones del Rover

void sonarBuzzer(bool encender) { 
  if (encender) {
    tone(PIN_BUZZER, FRECUENCIA_BUZZER); 
  } else {
    noTone(PIN_BUZZER); 
    digitalWrite(PIN_BUZZER, LOW); 
  }
}

// Función para medir la distancia
int medirDistancia() {
  digitalWrite(PIN_TRIG, LOW);
  delayMicroseconds(5);
  digitalWrite(PIN_TRIG, HIGH);
  delayMicroseconds(10);
  digitalWrite(PIN_TRIG, LOW);

  // pulseIn espera el pulso HIGH. Ponemos un timeout de 30000us (30ms) 
  // para que no se congele el robot si no hay eco (aprox 5 metros de alcance máximo)
  duracionUltrasonido = pulseIn(PIN_ECHO, HIGH, 30000);
  
  if (duracionUltrasonido == 0) {
    return -1; // Retorna -1 si no detecta nada (fuera de rango o error)
  }
  
  return (int)(duracionUltrasonido / 59);
}

void detener() { 
  motor1.setSpeed(0); 
  motor1.run(RELEASE); 
  motor2.setSpeed(0);
  motor2.run(RELEASE);
  motor3.setSpeed(0);
  motor3.run(RELEASE);
  motor4.setSpeed(0);
  motor4.run(RELEASE);

  if (caraActual != 'S') {
    caraNeutra();
    caraActual = 'S';
  }
}

// Condicion 1: si avanzamos y el ultrasónico ve algo a menos de 20 cm, parar
void verificarObstaculo() { 
  // distancia = -1 significa "sin eco", no debe interpretarse como obstáculo
  if (movimiento == 'F' && distancia > 0 && distancia < UMBRAL_DISTANCIA_SEGURIDAD) {
    movimiento = 'S'; // Evita que un cambio de velocidad vuelva a arrancar hacia el obstáculo
    detener();        // Motores en RELEASE y cara neutra
  }
}

// Condiciones 2 y 3: faros delanteros con poca luz y traseros al retroceder
void actualizarLuces() { 
  // Amarillos (delanteros): los enciende la app (W1) o la falta de luz
  bool encenderDelanteros = lucesAppEncendidas || (valLuz < UMBRAL_LUZ_BAJA);
  digitalWrite(PIN_LED_AMARILLO_1, encenderDelanteros ? HIGH : LOW);
  digitalWrite(PIN_LED_AMARILLO_2, encenderDelanteros ? HIGH : LOW);

  // Rojos (traseros): solo mientras el rover retrocede
  bool encenderTraseros = (movimiento == 'B') && !parada && velocidad > 0;
  digitalWrite(PIN_LED_ROJO_1, encenderTraseros ? HIGH : LOW);
  digitalWrite(PIN_LED_ROJO_2, encenderTraseros ? HIGH : LOW);
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
    case 'F': 
        mover(FORWARD, BACKWARD, BACKWARD, FORWARD);
        if (caraActual != 'F') {
          caraFeliz();
          caraActual = 'F';
          }
        break;
    case 'B': 
        mover(BACKWARD, FORWARD, FORWARD, BACKWARD);
        if (caraActual != 'B') {
          caraEnojada();
          caraActual = 'B';
          }
        break;
    case 'L': 
        mover(BACKWARD, BACKWARD, BACKWARD, BACKWARD);
        if (caraActual != 'L') {
          caraSorprendidaIzquierda();
          caraActual = 'L';
          }
        break;
    case 'R': 
        mover(FORWARD, FORWARD, FORWARD, FORWARD);
        if (caraActual != 'R') {
          caraSorprendidaDerecha();
          caraActual = 'R';
          }
        break;
    default: 
        detener();
        break;
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

  if (operacion == 'B' && (comando[1] == '1' || comando[1] == '0')) {
    sonarBuzzer(comando[1] == '1');
    return;
  }

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
      sonarBuzzer(false); 
      break;

    case 'W': 
      lucesAppEncendidas = (comando[1] == '1'); // Guarda el pedido de la app
      actualizarLuces(); // Se aplica enseguida (con poca luz se mantienen encendidos)
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

void enviarTelemetria() { 
  unsigned long ahora = millis();
  if (ahora - ultimaTelemetria < INTERVALO_TELEMETRIA) { 
    return;
  }
  ultimaTelemetria = ahora;

  // 1. Leer Temperatura
  a = analogRead(pinTemperatura);
  if (a == 0) a = 1; 
  resistencia = (float)(1023 - a) * 10000 / a;
  temperatura = 1 / (log(resistencia / 10000) / B + 1 / 298.15) - 273.15;

  // 2. Leer Luz
  valLuz = 1023 - analogRead(PIN_SENSOR_LUZ);

  // 3. Leer Distancia
  distancia = medirDistancia();

  // 4. Enviar JSON con los tres valores
  bluetoothSerial.print("{\"t\":"); 
  bluetoothSerial.print(temperatura, 1); 
  bluetoothSerial.print(",\"l\":"); 
  bluetoothSerial.print(valLuz); 
  bluetoothSerial.print(",\"d\":"); // Nueva clave de distancia
  bluetoothSerial.print(distancia); 
  bluetoothSerial.println("}"); 
}

void setup() { 
  bluetoothSerial.begin(9600); 
  // Serial.begin(9600); // Descomenta esta línea si quieres ver los datos en el Monitor Serie

  // Inicializar Pines de LEDs
  pinMode(PIN_LED_AMARILLO_1, OUTPUT);
  pinMode(PIN_LED_AMARILLO_2, OUTPUT);
  pinMode(PIN_LED_ROJO_1, OUTPUT);
  pinMode(PIN_LED_ROJO_2, OUTPUT);
  
  digitalWrite(PIN_LED_AMARILLO_1, LOW);
  digitalWrite(PIN_LED_AMARILLO_2, LOW);
  digitalWrite(PIN_LED_ROJO_1, LOW);
  digitalWrite(PIN_LED_ROJO_2, LOW);

  // Inicializar Buzzer
  pinMode(PIN_BUZZER, OUTPUT);
  sonarBuzzer(false);

  // Inicializar Sensor Ultrasónico
  pinMode(PIN_TRIG, OUTPUT);
  pinMode(PIN_ECHO, INPUT);
  digitalWrite(PIN_TRIG, LOW);

  // Inicializar OLED
  if(!display.begin(SSD1306_SWITCHCAPVCC, 0x3C)) { 
    // Si falla, continúa
  } else {
    display.clearDisplay();
    display.setTextColor(SSD1306_WHITE);
    caraNeutra(); 
    caraActual = 'S';
  }

  detener(); 
}

void loop() { 
  leerBluetooth();      // Atiende comandos de la app
  enviarTelemetria();   // Actualiza distancia y luz, y publica el JSON
  verificarObstaculo(); // Condicion 1: frena si hay algo a menos de 20 cm
  actualizarLuces();    // Condiciones 2 y 3: faros delanteros y traseros
}