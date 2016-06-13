
#include <SoftwareSerial.h>

#define RxD 13
#define TxD 12


SoftwareSerial BTSerial(RxD, TxD);
char c;
void setup()
{   
  c = '0';
   pinMode(7, OUTPUT);
  digitalWrite(7,HIGH);
  pinMode(6, OUTPUT);
  digitalWrite(6,HIGH);
  
  //AT configuration
  BTSerial.flush();
  delay(500);
  BTSerial.begin(9600);
  Serial.begin(9600);
  Serial.println("Preparado para enviar comandos AT:");
  BTSerial.print("ATrn");

  delay(100);

}

void loop()
{
  if (BTSerial.available()){
    c = (char)BTSerial.read(); 
    Serial.write(c);
    
    if(c == '1'){
      digitalWrite(7,LOW);
      delay(1000);
      digitalWrite(7,HIGH);
      BTSerial.write('7');
    }
    if(c == '0'){
      digitalWrite(6,LOW);
      
    }
    
  }
  delay(100);

}
