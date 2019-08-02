#include <math.h>
#include "make_wav.h"
#include <stdio.h>
#include <string.h>
#include <time.h>
#include <iostream>
#include <fstream>
#include <vector>
#include <sstream>
 
#define S_RATE  (44100)
#define PI 3.141592654
#define TIME_STEP (S_RATE/64)
 
int genSamp(int num, std::ifstream& file);
std::vector<std::vector<float> > readCsv(std::ifstream& file);
/**
 * Calculates linear intermediate values from a vector of envelope points
 */
void generateEnvelope(std::vector<float> amplitudes, float* gen, int length);
 
int main(int argc, char ** argv)
{
    srand(time(0));

    std::ifstream* file = new std::ifstream();
    file->open(argv[1], std::ios::in);
    std::cout << "Opend File" << std::endl;

    genSamp(1, *file);


    file->close();
    delete file;
    return 0;
}

std::vector<std::vector<float> > readCsv(std::ifstream& file){
    std::vector<std::vector<float> > out;
    std::string temp, line, col;
    while(std::getline(file, line)){
        std::stringstream lstream(line);
        std::vector<float> tmpVect;
        while(std::getline(lstream, col, ',')){
           tmpVect.push_back(std::stof(col)); 
        }
        out.push_back(tmpVect);
    }
    return out;
}

void generateEnvelope(std::vector<float> amplitudes, float* retval, int length, int coeff){
    assert(length >= amplitudes.size());
    int scaling = length / amplitudes.size();
    int ival = 0;
    for(int point = 0; point < amplitudes.size(); point++){
        float delta;
        float value = amplitudes[point];
        if(point < amplitudes.size() - 1){
            delta = (amplitudes[point + 1] - amplitudes[point]) / scaling; //Delta Amp / Delta Time
        }
        else
            delta = (0 - amplitudes[point]) / scaling;

        int ivalstart = ival;
        while(ival < ivalstart + scaling){
            retval[ival] = value * coeff;
            value += delta;
            ival++;
        }
    }
}

std::vector<std::vector<float> > transposeVect(std::vector<std::vector<float> > in){
    std::vector<std::vector<float> > out;
    for(int c = 0; c < in[0].size(); c++){
        std::vector<float> vals;
        for(int r = 0; r < in.size(); r++){
            vals.push_back( in[r][c] );
        }
        out.push_back(vals);
    }
    return out;
}

int genSamp(int num, std::ifstream& file){
 
    long t = 0; 

    std::vector<std::vector<float> > csv(readCsv(file));
    std::vector<std::vector<float> > csvT = transposeVect(csv);
    
    std::cout << "Parsed CSV" << std::endl;
    
    int length = csv.size() * TIME_STEP;
    float A[length], C[length], D1[length], M1[length], D2[length], M2[length], D3[length], M3[length];
    generateEnvelope(csvT[0], A, length, 32000);
    generateEnvelope(csvT[1], C, length, (220 * 32));
    generateEnvelope(csvT[2], D1, length, 5);
    generateEnvelope(csvT[3], M1, length, (220 * 32));
    generateEnvelope(csvT[4], D2, length, 5);
    generateEnvelope(csvT[5], M2, length, (220 * 32));
    generateEnvelope(csvT[6], D3, length, 5);
    generateEnvelope(csvT[7], M3, length, (220 * 32));

    std::cout << "Generated Envelopes" << std::endl;

    short int buffer[length];
    for (int i=0; i<length; i++)
    {
        float env = (float)i / S_RATE;

        buffer[i] = (int)( A[i]  * sin( 2 * PI / S_RATE * C[i]  * t +
                    D1[i] * sin( 2 * PI / S_RATE * M1[i] * t +
                    D2[i] * sin( 2 * PI / S_RATE * M2[i] * t + 
                    D3[i] * sin( 2 * PI / S_RATE * M3[i] * t)))));
        t++;
    }

    char fname[50];
    sprintf(fname, "Evaluation%d.wav", num);
    write_wav(fname, length, buffer, S_RATE);

    return 0;
}

