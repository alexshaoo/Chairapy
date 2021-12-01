/*******
 * timed pressure plate
 * 
 * 
 * 
 * 
 */


#include <Stepper.h>

/* sitting reminders */
int analogPin = A5; // potentiometer wiper (middle terminal) connected to analog pin 5
                    // outside leads to ground and +3.3V
float val = 0.0;  // variable to store the value read
int count = 0;
int threshold = 150; //below threshold = sitting, every person's threshold is a little different
double lowBound = 1.0;
double highBound = 3.0;
float avg = 1024;

// readInterval*12  = 6 mins of readings
float readings[12] = {1024, 1024, 1024, 1024, 1024, 1024, 1024, 1024, 1024, 1024, 1024, 1024}; 

// readInterval*12/2 * 10 = 30 min of averages 
// from readInterval*12  = 6 min sliding window
// updated every readInterval*12/2 = 3 min
float history[10] = {1024, 1024, 1024, 1024, 1024, 1024, 1024, 1024, 1024, 1024}; 

const int stepsPerRevolution = 200;  // change this to fit the number of steps per revolution

// initialize the stepper library on pins 8 through 11:
Stepper myStepper(stepsPerRevolution, 8, 9, 10, 11);

int stepCount = 0;  // number of steps the motor has taken
int motorSpeed = 0;

unsigned long startTime;
unsigned long currentTime;
long halfminute = 30*1000;
unsigned long readInterval = 1*halfminute; // read every 30 seconds
unsigned long printInterval = halfminute/10; // print notification every 3 seconds

/* bluetooth */
String message; //string that stores the incoming message

void setup() {
  Serial.begin(9600);           //  setup serial
  Serial.println("test");
  startTime = millis();
  Serial.println(startTime);
}

void loop() {
  // get reading every readInterval
  currentTime = millis();
  if (currentTime - startTime >= readInterval) {
    startTime = currentTime;
    val = analogRead(analogPin); // read the input pin
    readings[count % 12] = val;
    count++;
    // rolling average
    if(count % 6 == 0){
        avg = average(readings, 10);
        Serial.println(avg);
        history[(count/6)%10]=avg;
    }
  }
  
  // if we've been sitting for the past 10*12*readInterval minutes and we are still sitting
  // the motor buzzes
  if(isSitting(history, 10) && (val < threshold)){
    motorSpeed = 200;
  } else {
    motorSpeed = 0;
  }
  setMotor(motorSpeed);

  // check for any bluetooth messages
  while(Serial.available())
  {//while there is data available on the serial monitor
    message+=char(Serial.read());//store string from serial command
  }
  if(!Serial.available())
  {
    if(message!="")
    {//if data is available
      readMsg(message);
    }
  }
}

/* sitting reminders */
float average (float * array, int len)  // assuming array is float.
{
  float sum = 0 ;  // sum will be larger than an item, long for safety.
  for (int i = 0 ; i < len ; i++)
    sum += array [i] ;
  return  ((float) sum) / len ;  // average will be fractional, so float may be appropriate.
}

void setMotor(int motorSpeed) {
  // set the motor speed
  if (motorSpeed > 0) {
    myStepper.setSpeed(motorSpeed);
    // step 1/100 of a revolution:
    myStepper.step(stepsPerRevolution / 100);
    
    currentTime = millis();
    // not precise but precision isn't that important here
    if (currentTime - startTime >= printInterval) { 
      Serial.println("Get up and stretch a little :D");
    }
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

void readMsg(String msg){
  //if message includes previous value (ie user is still scrolling)
  if(message.length() >5){
    message = message.substring(message.length() - 5);
  } 
  // values formatted <%3d>
  if(message.startsWith("<") && message.endsWith(">")){
    int newVal = message.substring(1, message.length()-1).toInt();
    if((newVal<121) && (newVal !=0)){
      readInterval = newVal*halfminute/30; // update readInterval
      message=""; //clear the data
    } 
  }
}
