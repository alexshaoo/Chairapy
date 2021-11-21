/*******
 * timed pressure plate
 * time at scale of 300x (ie 1s = 5 min)
 * 
 * 
 * 
 * 
 */


#include <Stepper.h>

int analogPin = A5; // potentiometer wiper (middle terminal) connected to analog pin 5
                    // outside leads to ground and +3.3V
float val = 0.0;  // variable to store the value read
int count = 0;
int threshold = 770;
double lowBound = 1.0;
double highBound = 3.0;
float avg = 3.3;
float readings[12] = {3.3, 3.3, 3.3, 3.3, 3.3, 3.3, 3.3, 3.3, 3.3, 3.3, 3.3, 3.3}; // 6 mins of readings
float history[10] = {3.3, 3.3, 3.3, 3.3, 3.3, 3.3, 3.3, 3.3, 3.3, 3.3}; // 30 min of averages from 6 min sliding window, updated every 3 min

const int stepsPerRevolution = 200;  // change this to fit the number of steps per revolution
// for your motor


// initialize the stepper library on pins 8 through 11:
Stepper myStepper(stepsPerRevolution, 8, 9, 10, 11);

int stepCount = 0;  // number of steps the motor has taken
int motorSpeed = 0;

unsigned long startTime;
unsigned long currentTime;
unsigned short readInterval = 100;


float average (float * array, int len)  // assuming array is float.
{
  float sum = 0 ;  // sum will be larger than an item, long for safety.
  for (int i = 0 ; i < len ; i++)
    sum += array [i] ;
  return  ((float) sum) / len ;  // average will be fractional, so float may be appropriate.
}

void setup() {
  Serial.begin(9600);           //  setup serial
  Serial.println("test");
  startTime = millis();
  Serial.println(startTime);
}

void loop() {
  currentTime = millis();
  if (currentTime - startTime >= readInterval) {
    startTime = currentTime;
    
    val = analogRead(analogPin);//*3.3/1023 - lowBound;  // read the input pin
    readings[count % 12] = val;
    count++;
    // average calculated every 3 minutes
    if(count % 6 == 0){
        avg = average(readings, 10);
        Serial.println(avg);
        history[(count/6)%10]=avg;
    }
    
  }
  if(isSitting(history, 10) && (val < threshold)){
    motorSpeed = 100;
  } else {
    motorSpeed = 0;
  }
  if (motorSpeed > 0) {
    myStepper.setSpeed(motorSpeed);
    // step 1/100 of a revolution:
    myStepper.step(stepsPerRevolution / 100);
    //Serial.println("Get up and stretch a little :D");
  }
}


void setMotor(int motorSpeed) {
//  // read the sensor value:
//  int sensorReading = analogRead(A0);
//  // map it to a range from 0 to 100:
//  int motorSpeed = map(sensorReading, 0, 1023, 0, 100);
  // set the motor speed:
  if (motorSpeed > 0) {
    myStepper.setSpeed(motorSpeed);
    // step 1/100 of a revolution:
    myStepper.step(stepsPerRevolution / 100);
  }
}

bool isSitting(float * readingsHistory, int len){
  for(int i=0; i<len; ++i){
    if(readingsHistory[i] > threshold){
      return false;
    }
  }
  return true;
}
