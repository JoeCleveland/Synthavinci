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

public class FMSynthSimple {
    public static final int NUM_SAMPLES = 20000;

    public static void main(String[] args) {
        Random rand = new Random();
        for(int i = 0; i < NUM_SAMPLES; i++){

            double[] outParams = new double[]{rand.nextInt(32),
                    rand.nextDouble(), rand.nextDouble(), rand.nextDouble(), rand.nextDouble(),
                    rand.nextDouble(), rand.nextDouble(), rand.nextDouble(),
                    rand.nextDouble(), rand.nextDouble(), rand.nextDouble(), rand.nextDouble(),
                    rand.nextDouble(), rand.nextDouble(), rand.nextDouble(),
                    rand.nextDouble()};

            double[] params = new double[]{
                    440, outParams[0] * 220,

                    outParams[1], outParams[2], outParams[3],//Levels 0 - 1
                    outParams[4]/4, outParams[5]/4, outParams[6]/2, outParams[7]/2,

                    outParams[8], outParams[9], outParams[10],
                    outParams[11]/4, outParams[12]/4, outParams[13]/2, outParams[14]/2,

                    outParams[15]};
            buildSynth(params, i);

            try {
                FileWriter fw = new FileWriter("/users/josephcleveland/FMData/mat.txt", true);
                DecimalFormat df = new DecimalFormat("0.000");
                String s = "";
                for (int j = 0; j < 16; j++) {
                    s += df.format(outParams[j]);
                    if (j < 15)
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
     *
     * 0, 1 Opfreq
     * 2, 3, 4, 5, 6, 7, 8 Op1 Env Al, Dl, Sl, At, Dt, St, Rt
     * 9, 10, 11, 12, 13, 14, 15 Op2 Env Al, Dl, Sl, At, Dt, St, Rt
     * 16 Feedback amnt
     * @param params
     */
    public static void buildSynth(double[] params, int num){
        if(params.length != 17)
            throw new IllegalArgumentException("Not right num of params");

        Synthesizer synth = JSyn.createSynthesizer();

        LineOut myOut = new LineOut();
        synth.add(myOut);

        SineOscillatorPhaseModulated op1 = new SineOscillatorPhaseModulated();
        op1.frequency.set(params[0]);
        //op1.amplitude.set(0);
        SineOscillatorPhaseModulated op2 = new SineOscillatorPhaseModulated();
        op2.frequency.set(params[1]);
        //op2.amplitude.set(0);

        Multiply feedback = new Multiply();
        feedback.inputA.set(params[16]);
        synth.add(feedback);

        synth.add(op1);
        synth.add(op2);

        op1.output.connect(myOut);
        op2.output.connect(op1.modulation);
        op2.output.connect(feedback.inputB);
        feedback.output.connect(op2.modulation);



        double scaling = 1.0;

        VariableRateDataReader env1 = addEnvelopeForData(Arrays.copyOfRange(params, 2, 9), scaling, synth);
        VariableRateDataReader env2 = addEnvelopeForData(Arrays.copyOfRange(params, 9, 16), scaling, synth);

        env1.output.connect(op1.amplitude);
        env2.output.connect(op2.amplitude);



        //myOut.start();


        File waveFile = new File( "/Users/josephcleveland/FMData/samp" + (num) + ".wav" );
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


        synth.stop();
    }

    public static VariableRateDataReader addEnvelopeForData(double[] env, double scaling, Synthesizer synth){
        double[] data = new double[]{
                0.01, 0,
                env[3], env[0] * scaling,
                env[4], env[1] * scaling,
                env[5], env[2] * scaling,
                env[6], 0};
        SegmentedEnvelope myEnvData = new SegmentedEnvelope( data );
        VariableRateDataReader envPlayer = new VariableRateMonoReader();
        envPlayer.dataQueue.clear( );
        envPlayer.dataQueue.queue( myEnvData, 0, myEnvData.getNumFrames() );
        synth.add(envPlayer);

        return envPlayer;
    }

}
