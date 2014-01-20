#include <Wire.h>

#define CTRL_REG1 0x20
#define CTRL_REG2 0x21
#define CTRL_REG3 0x22
#define CTRL_REG4 0x23
#define CTRL_REG5 0x24

char sprintBuffer[64];

byte L3G4200D_address = 0b1101000;

int x, y, z;

byte i2c_address, i2c_register, i2c_value;

void setup()
{
  // start i2c, automatically enables pullups to 5V
  Wire.begin();
  // turn off internal i2c pullups, external pullups to 3.3V are on the sensor
  pinMode(2, INPUT);
  pinMode(3, INPUT);
  Serial.begin(9600);
  // configure L3G4200  - 250, 500 or 2000 deg/sec
  setupL3G4200D(250);
  // wait for the sensor to be ready 
  delay(1500);
}

void loop()
{
  if(Serial.available())
  {
    switch(Serial.read())
    {
    case 's':
      // configure L3G4200
      setupL3G4200D(2000);
      break;
    case 'g':
      // update x, y, and z with new values
      getGyroValues();
      // sprintf does not support float values, so send raw values
      sprintf(sprintBuffer, "[%d,%d,%d]", x, y, z);
      Serial.print(sprintBuffer);
      break;
    case 'r':
      // read from i2c
      i2c_address = Serial.read();
      i2c_register = Serial.read();
      Serial.write(readRegister(i2c_address, i2c_register));
      break;
    case 'w':
      // write to i2c
      i2c_address = Serial.read();
      i2c_register = Serial.read();
      i2c_value = Serial.read();
      writeRegister(i2c_address, i2c_register, i2c_value);
      break;
    }
  }
}

void getGyroValues()
{
  byte xMSB = readRegister(L3G4200D_address, 0x29);
  byte xLSB = readRegister(L3G4200D_address, 0x28);
  x = ((xMSB << 8) | xLSB);

  byte yMSB = readRegister(L3G4200D_address, 0x2B);
  byte yLSB = readRegister(L3G4200D_address, 0x2A);
  y = ((yMSB << 8) | yLSB);

  byte zMSB = readRegister(L3G4200D_address, 0x2D);
  byte zLSB = readRegister(L3G4200D_address, 0x2C);
  z = ((zMSB << 8) | zLSB);
}

int setupL3G4200D(int scale)
{
  // From  Jim Lindblom of Sparkfun's code

  // Enable x, y, z and turn off power down:
  writeRegister(L3G4200D_address, CTRL_REG1, 0b00001111);

  // If you'd like to adjust/use the HPF, you can edit the line below to configure CTRL_REG2:
  writeRegister(L3G4200D_address, CTRL_REG2, 0b00000000);

  // Configure CTRL_REG3 to generate data ready interrupt on INT2
  // No interrupts used on INT1, if you'd like to configure INT1 or INT2 otherwise, consult the datasheet:
  //writeRegister(L3G4200D_address, CTRL_REG3, 0b00001000);

  // CTRL_REG4 controls the full-scale range, among other things:

  if(scale == 250)
  {
    writeRegister(L3G4200D_address, CTRL_REG4, 0b00000000);
  }
  else if(scale == 500)
  {
    writeRegister(L3G4200D_address, CTRL_REG4, 0b00010000);
  }
  else
  {
    writeRegister(L3G4200D_address, CTRL_REG4, 0b00110000);
  }

  // CTRL_REG5 controls high-pass filtering of outputs, use it
  // if you'd like:
  writeRegister(L3G4200D_address, CTRL_REG5, 0b00000000);
}

void writeRegister(byte address, byte registerAddress, byte val)
{
  Wire.beginTransmission(address); 
  Wire.write(registerAddress);
  Wire.write(val);
  Wire.endTransmission();
}

int readRegister(int address, byte registerAddress)
{
  Wire.beginTransmission(address);
  Wire.write(registerAddress);
  Wire.endTransmission();

  Wire.requestFrom(address, 1);
  
  // waiting
  while(!Wire.available());
  return Wire.read();
}

