import com.jsyn.JSyn;
import com.jsyn.Synthesizer;
import com.jsyn.data.SegmentedEnvelope;
import com.jsyn.unitgen.*;
import com.jsyn.util.WaveRecorder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Random;

public class FMSynthMultienvelope {
    public static final int NUM_SAMPLES = 80000;

    public static void main(String[] args) {
        //Random rand = new Random();
        for(int samp = 0; samp < NUM_SAMPLES; samp++){
            Random rand = new Random();
            double[][] params = new double[5][64];

            double v1 = rand.nextDouble();
            double v2 = rand.nextDouble();
            double v3 = rand.nextDouble();
            double v4 = rand.nextDouble();
            double v5 = rand.nextDouble();
            for(int i = 0; i < 64; i++){
                params[0][i] = v1;
            }
            for(int i = 0; i < 64; i++){
                params[1][i] = v2;
            }
            for(int i = 0; i < 64; i++){
                params[2][i] = v3;
            }
            for(int i = 0; i < 64; i++){
                params[3][i] = v4;
            }
            for(int i = 0; i < 64; i++){
                params[4][i] = v5;
            }
            buildSynth(params, samp);

            double[] outParams = new double[]{v1, v2, v3, v4, v5};
            try {
                FileWriter fw = new FileWriter("/users/josephcleveland/MoreFMData/mat.txt", true);
                DecimalFormat df = new DecimalFormat("0.000");
                String s = "";
                for (int j = 0; j < 5; j++) {
                    s += df.format(outParams[j]);
                    if (j < 4)
                        s += ", ";
                }
                s += "\n";
                fw.append(s);
                fw.close();
            } catch (IOException e) {
                System.out.println(e);
            }
        }
    }

    /**
     * 64 time steps - each with 4 params
     * op1, op2, op1Freq, op2Freq, feedback
     *
     * params is 5 * 64 array
     *
     * @param params
     */
    public static void buildSynth(double[][] params, int num){
        if(params.length != 5)
            throw new IllegalArgumentException("Not right num of params");

        Synthesizer synth = JSyn.createSynthesizer();

        LineOut myOut = new LineOut();
        synth.add(myOut);

        SineOscillatorPhaseModulated op1 = new SineOscillatorPhaseModulated();
        op1.frequency.set(440);
        SineOscillatorPhaseModulated op2 = new SineOscillatorPhaseModulated();

        Multiply feedback = new Multiply();
        synth.add(feedback);

        synth.add(op1);
        synth.add(op2);

        op1.output.connect(myOut);
        op2.output.connect(op1.modulation);
        op2.output.connect(feedback.inputB);
        feedback.output.connect(op2.modulation);

        double time = 4.0 / 64;
        VariableRateDataReader env1 = addEnvelopeForData(params[0], 1.0, synth, time);
        VariableRateDataReader env2 = addEnvelopeForData(params[1], 1.0, synth, time);
        VariableRateDataReader env3 = addEnvelopeForData(params[2], 220 * 32, synth, time);
        VariableRateDataReader env4 = addEnvelopeForData(params[3], 220 * 32, synth, time);
        VariableRateDataReader env5 = addEnvelopeForData(params[4], 1.0, synth, time);

        env1.output.connect(op1.amplitude);
        env2.output.connect(op2.amplitude);
        env3.output.connect(op1.frequency);
        env4.output.connect(op2.frequency);
        env5.output.connect(feedback.inputA);

        synth.start();

        op1.start();
        op2.start();

        env1.start();
        env2.start();
        myOut.start();

/*
        File waveFile = new File( "/Users/josephcleveland/MoreFMData/samp" + (num) + ".wav" );
        try {
            WaveRecorder recorder = new WaveRecorder(synth, waveFile);
            op1.output.connect(0, recorder.getInput(), 0 );
            recorder.start();
            synth.start();

            op1.start();
            op2.start();

            env1.start();
            env2.start();
            synth.sleepFor(1.5);
            recorder.stop();

        } catch(FileNotFoundException | InterruptedException e2) {}
*/

        //synth.stop();
    }

    public static VariableRateDataReader addEnvelopeForData(double[] env, double scaling, Synthesizer synth, double time){
        //time, value
        double[] data = new double[130];//128 + 2 more for the finale
        for(int i = 0; i < 128; i += 2){
            data[i] = time;
            data[i + 1] = scaling * env[i / 2];
        }
        data[128] = time;
        data[129] = 0;
        SegmentedEnvelope myEnvData = new SegmentedEnvelope( data );
        VariableRateDataReader envPlayer = new VariableRateMonoReader();
        envPlayer.dataQueue.clear( );
        envPlayer.dataQueue.queue( myEnvData, 0, myEnvData.getNumFrames() );
        synth.add(envPlayer);

        return envPlayer;
    }
}
