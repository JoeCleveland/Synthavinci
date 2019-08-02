#include <math.h>
#include "make_wav.h"
#include <stdio.h>
#include <string.h>
#include <time.h>
#include <iostream>
#include <fstream>
 
#define S_RATE  (44100)
#define BUF_SIZE (S_RATE/4) /* 0.25 second buffer */
#define PI 3.141592654
#define NUM_SAMPS 200000
 
short int buffer[BUF_SIZE];
int genSamp(int num, std::ofstream& file);
 
int main(int argc, char ** argv)
{
    srand(time(0));

    std::ofstream* file = new std::ofstream();
    file->open("mat.txt", std::ios::app);

    for(int i = 0; i < NUM_SAMPS; i++){
        genSamp(i, *file);
        if(i % 1000 == 0)
            printf("%d\n", i);
    }

    file->close();
    delete file;
    return 0;
}

int genSamp(int num, std::ofstream& file){
    int i;
 
    long t = 0; 

    float A = rand() % 32000;
    //inject random silence
    if(rand()%500 == 1){
        A = 0;
    }
    float C = rand() % (220 * 32);
    float D1 = (float)(rand() % 1000) / 200;
    float M1 = (rand() % 32) * 220;
    float D2 = (float)(rand() % 1000) / 200;
    float M2 = (rand() % 32) * 220;
    float D3 = (float)(rand() % 1000) / 200;
    float M3 = (rand() % 32) * 220;

    for (i=0; i<BUF_SIZE; i++)
    {
        float env = (float)i / S_RATE;
        buffer[i] = (int)( A  * sin( 2 * PI / S_RATE * C  * t +
                    D1 * sin( 2 * PI / S_RATE * M1 * t +
                    D2 * sin( 2 * PI / S_RATE * M2 * t + 
                    D3 * sin( 2 * PI / S_RATE * M3 * t)))));
        t++;
    }

    char fname[50];
    sprintf(fname, "Samps/samp%d.wav", num);
    write_wav(fname, BUF_SIZE, buffer, S_RATE);

    A  /= 32000;
    C  /= (220 * 32.0);
    D1 /= 5.0;
    M1 /= (220 * 32.0);
    D2 /= 5.0;
    M2 /= (220 * 32.0);
    D3 /= 5.0;
    M3 /= (220 * 32.0);


    file << A << ", " << C << ", " << D1 << ", " << M1 << ", " << D2 << ", " << M2 << ", " << D3 << ", " << M3 << "\n"; 
    return 0;
}
