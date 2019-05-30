import com.jsyn.*;
import com.jsyn.data.SegmentedEnvelope;
import com.jsyn.unitgen.*;
import com.jsyn.unitgen.SineOscillator;
import com.jsyn.unitgen.TriangleOscillator;
import com.jsyn.unitgen.SquareOscillator;
import com.jsyn.unitgen.SawtoothOscillator;
import com.jsyn.unitgen.FilterLowPass;
import com.jsyn.util.WaveRecorder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;


public class Synth {

    public static void main(String[] args) {
        Random rand = new Random();

        for(int run = 0; run < 55000; run++) {
            int[] classes = new int[23];
            for (int i = 0; i < 23; i++) {
                classes[i] = rand.nextInt(16);
                System.out.println(classes[i]);
            }
            double h[] = {valueForFreqClass(classes[0]),
                    valueForFreqClass(classes[1]),
                    valueForFreqClass(classes[2]),
                    valueForFreqClass(classes[3]),
                    valueForModFreqClass(classes[4]),
                    valueForModFreqClass(classes[5]),
                    valueForModFreqClass(classes[6]),
                    valueForModFreqClass(classes[7]),
                    valueForCarrierAmplitudeClass(classes[8]),
                    valueForCarrierAmplitudeClass(classes[9]),
                    valueForCarrierAmplitudeClass(classes[10]),
                    valueForCarrierAmplitudeClass(classes[11]),
                    valueForModulationAmplitudeClass(classes[12]),
                    valueForModulationAmplitudeClass(classes[13]),
                    valueForModulationAmplitudeClass(classes[14]),
                    valueForModulationAmplitudeClass(classes[15]),
                    valueForEnvelopeClass(classes[16]),
                    valueForEnvelopeClass(classes[17]),
                    valueForSustainAmplitudeClass(classes[18]),
                    valueForEnvelopeClass(classes[19]),
                    valueForCutoffFreqClass(classes[20]),
                    valueForQClass(classes[21]),
                    valueForSustainTimeClass(classes[22])};
            buildSynth(h, run);

            try {
                FileWriter fw = new FileWriter("/Users/josephcleveland/dataset/mat.txt", true);
                String s = "";
                for (int i = 0; i < 23; i++) {
                    s += classes[i];
                    if (i < 22)
                        s += ", ";
                }
                s += "\n";
                fw.append(s);
                fw.close();
            } catch (IOException e) {
                System.out.println(e);
            }
        }
        System.exit(0);
    }

    public static double valueForFreqClass(int c){
        return Math.pow(2, c / 12) * 440.0;
    }

    public static double valueForModFreqClass(int c){
        return 1 + c * 1.825;
    }

    public static double valueForCarrierAmplitudeClass(int c){
        return 0.001 + c * 0.0624375;
    }

    public static double valueForModulationAmplitudeClass(int c){
        return c * 93.75;
    }

    public static double valueForEnvelopeClass(int c){
        return 0.001 + 0.01 * c;
    }

    public static double valueForSustainTimeClass(int c){
        return 0.001 + 0.03125 * c;
    }

    public static double valueForSustainAmplitudeClass(int c){
        if(c == 0)
            return 0;
        else
            return 1 / c;
    }

    public static double valueForCutoffFreqClass(int c){
        return 200 + 237.5 * c;
    }

    public static double valueForQClass(int c){
        return 0.01 + 0.061875 * c;
    }
    /**
     * h[0 - 3] carrier frequencies
     * h[4 - 7] modulation frequencies
     * h[8 - 11] carrier amplitudes
     * h[12 - 15] modulation amplitudes
     * h[16 - 19] - ADSR values
     * h[20] - LPF cutoff
     * h[21] - LPF Q
     * h[22] - Sustain time
     * @param h
     */
    public static void buildSynth(double[] h, int num){
        Synthesizer synth = JSyn.createSynthesizer();

        LineOut myOut;
        SineOscillatorPhaseModulated oscSine;
        TriangleOscillator oscTri;
        SquareOscillator oscSquare;
        SawtoothOscillator oscSaw;
        synth.add(myOut = new LineOut());
        synth.add(oscSine = new SineOscillatorPhaseModulated());
        synth.add(oscTri = new TriangleOscillator());
        synth.add(oscSquare = new SquareOscillator());
        synth.add(oscSaw = new SawtoothOscillator());

        SineOscillator fm1;
        synth.add(fm1 = new SineOscillator());
        fm1.frequency.set(h[4]);//First modulation frequency
        fm1.amplitude.set(h[12]);//First modulation amplitude

        Add a1 = new Add();
        a1.inputA.set(h[0]);//First carrier frequency
        fm1.output.connect(a1.inputB);
        a1.output.connect(oscSine.frequency);

        SineOscillator fm2;
        synth.add(fm2 = new SineOscillator());
        fm2.frequency.set(h[5]);//Second modulation frequency
        fm2.amplitude.set(h[13]);//Second modulation amplitude

        Add a2 = new Add();
        a2.inputA.set(h[1]);//Second carrier frequency
        fm2.output.connect(a2.inputB);
        a2.output.connect(oscTri.frequency);

        SineOscillator fm3;
        synth.add(fm3 = new SineOscillator());
        fm3.frequency.set(h[6]);
        fm3.amplitude.set(h[14]);

        Add a3 = new Add();
        a3.inputA.set(h[2]);
        fm3.output.connect(a3.inputB);
        a3.output.connect(oscSquare.frequency);

        SineOscillator fm4;
        synth.add(fm4 = new SineOscillator());
        fm4.frequency.set(h[7]);
        fm4.amplitude.set(h[15]);

        Add a4 = new Add();
        a4.inputA.set(h[3]);
        fm4.output.connect(a4.inputB);
        a4.output.connect(oscSaw.frequency);

        double[] data =
                {
                        0.02, 0.0,      // Start (Time, Value)
                        h[16], 1.0,     //Attack
                        h[17], h[18],   // Decay to the sustain value
                        h[22], h[18],   // Hold the sustain value for sustain time
                        h[19], 0.0,     //Release
                };
        SegmentedEnvelope myEnvData = new SegmentedEnvelope( data );
        VariableRateDataReader envPlayer = new VariableRateMonoReader();
        envPlayer.dataQueue.clear( );
        envPlayer.dataQueue.queue( myEnvData, 0, myEnvData.getNumFrames() );
        synth.add(envPlayer);

        FilterLowPass flp = new FilterLowPass();
        synth.add(flp);

        flp.frequency.set(h[20]);
        flp.Q.set(h[21]);

        oscSine.amplitude.set(h[8]);
        oscTri.amplitude.set(h[9]);
        oscSquare.amplitude.set(h[10]);
        oscSaw.amplitude.set(h[11]);

        oscSine.output.connect(flp.input);
        oscTri.output.connect(flp.input);
        oscSquare.output.connect(flp.input);
        oscSaw.output.connect(flp.input);

        envPlayer.output.connect(flp.amplitude);

        //flp.output.connect(myOut.input);

        File waveFile = new File( "/Users/josephcleveland/dataset/samp" + (6000 + num) + ".wav" );
        try {
            WaveRecorder recorder = new WaveRecorder(synth, waveFile);
            flp.output.connect(0, recorder.getInput(), 0 );
            recorder.start();
            synth.start();
            myOut.start();
            envPlayer.start();
            Thread.sleep(1000);
            recorder.stop();

        } catch(FileNotFoundException | InterruptedException e2) {}

        synth.stop();
    }
}
