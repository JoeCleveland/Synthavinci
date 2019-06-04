import com.jsyn.JSyn;
import com.jsyn.Synthesizer;
import com.jsyn.data.SegmentedEnvelope;
import com.jsyn.unitgen.*;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;


public class AdditiveEnvelopeSynth {

    /**
     * freq_env.csv
     * amp_env.csv
     */
    public static void main(String[] args) throws InterruptedException{
        String freqcsv = args[0];
        String ampcsv = args[1];
        int frameLength = Integer.parseInt(args[2]);
        ArrayList<double[]> freq_env_csv = readcsv(freqcsv);
        ArrayList<double[]> amp_env_csv = readcsv(ampcsv);
        int num_osc = freq_env_csv.get(0).length;


        ArrayList<double[]> freq_env = csvToEnvs(freq_env_csv, num_osc, frameLength);
        ArrayList<double[]> amp_env = csvToEnvs(amp_env_csv, num_osc, frameLength);
        Synthesizer synth = JSyn.createSynthesizer();
        LineOut out = new LineOut();
        synth.add(out);
        out.start();


        for(int oscI = 0; oscI < num_osc; oscI++){

            SineOscillator o = new SineOscillatorPhaseModulated();
            o.output.connect(out);

            SegmentedEnvelope freqEnvData = new SegmentedEnvelope( freq_env.get(oscI) );
            VariableRateDataReader freqEnvPlayer = new VariableRateMonoReader();
            freqEnvPlayer.dataQueue.clear( );
            freqEnvPlayer.dataQueue.queue( freqEnvData, 0, freqEnvData.getNumFrames() );
            synth.add(freqEnvPlayer);

            freqEnvPlayer.output.connect(o.frequency);

            SegmentedEnvelope ampEnvData = new SegmentedEnvelope( amp_env.get(oscI) );
            VariableRateDataReader ampEnvPlayer = new VariableRateMonoReader();
            ampEnvPlayer.dataQueue.clear( );
            ampEnvPlayer.dataQueue.queue( ampEnvData, 0, ampEnvData.getNumFrames() );
            synth.add(ampEnvPlayer);

            ampEnvPlayer.output.connect(o.amplitude);

            synth.add(o);

            o.start();
            freqEnvPlayer.start();
            ampEnvPlayer.start();
        }

        synth.start();
        System.out.println("Press Ctrl-C to kill");
        while(true) {}
        //synth.stop();

    }

    public static ArrayList<double[]> readcsv(String f){
        BufferedReader br = null;
        String line = "";
        String cvsSplitBy = ",";
        ArrayList<double[]> output = new ArrayList<>();
        try {

            br = new BufferedReader(new FileReader(f));
            while ((line = br.readLine()) != null) {

                String[] l = line.split(cvsSplitBy);
                double[] numLine = new double[l.length];
                for(int i = 0; i < l.length; i++)
                    numLine[i] = Double.parseDouble(l[i]);

                output.add(numLine);
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return output;
    }

    public static ArrayList<double[]> csvToEnvs(ArrayList<double[]> csvdata, int numOsc, double frameLength){
        int envLength = csvdata.size() * 2;
        ArrayList<double[]> output = new ArrayList<>();

        for(int oscIndex = 0; oscIndex < numOsc; oscIndex++) {
            double[] env = new double[envLength];
            for (int timeIndex = 0; timeIndex < envLength; timeIndex++) {
                if(timeIndex % 2 == 0){
                    //if(timeIndex == 0)
                    //    frameLength = 0.001; //Don't attack into first frame
                    env[timeIndex] = frameLength;
                }else{
                    env[timeIndex] = csvdata.get(timeIndex/2)[oscIndex];
                }
            }
            output.add(env);
        }
        return output;
    }
}