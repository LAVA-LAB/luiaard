package prism;

import java.util.ArrayList;
import java.util.HashMap;
import java.lang.Math;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;



public class DataProcessor {
    

    private ArrayList<DataPoint> averages;
    private ArrayList<Double> variances;
    private ArrayList<Double> standardDeviations;
    private ArrayList<DataPoint> upperConfidences;
    private ArrayList<DataPoint> lowerConfidences;

    private ArrayList<Double> minima;
    private ArrayList<Double> maxima;
    private ArrayList<Double> modi;

    private String type = "";
    private double optimum = 0; 

    private String axis = "";
    private String plots = "";
    private String legend = "";

    private String[] colors = {"WildStrawberry","Cerulean","Green","BurntOrange"};

    private boolean fullDocument = true;

    public DataProcessor() {
        this.averages = new ArrayList<>();
        this.variances = new ArrayList<>();
        this.standardDeviations = new ArrayList<>();
        this.upperConfidences = new ArrayList<>();
        this.lowerConfidences = new ArrayList<>();
    }

    public DataProcessor(String type, double sulOpt) {
        this.type = type;
        this.optimum = sulOpt;
    }


    public void computeAverages(ArrayList<ArrayList<DataPoint>> repeatedData, int repetitions, int iterations) {
        averages = new ArrayList<>();
        for (int i = 0; i < iterations; i++) {
            double sum = 0.0;
            int observations = 0;
            for (int r = 0; r < repetitions; r++) {
                double value = repeatedData.get(r).get(i).getValue();
                sum += value;
                int obs = repeatedData.get(r).get(i).getAccumulatedSamples();
                observations += obs;
            }
            double average = sum / repetitions;
            observations = observations / repetitions;
            DataPoint p = new DataPoint(observations, average);
            averages.add(p);
        }
    }

    public void computeConfidenceIntervals(ArrayList<ArrayList<DataPoint>> repeatedData, int repetitions, int iterations, int confidenceLevel) {
        upperConfidences = new ArrayList<>();
        lowerConfidences = new ArrayList<>();
        double z = 0.0;
        if (confidenceLevel == 90) {
            z = 1.645;
        }
        else if (confidenceLevel == 95) { 
            z = 1.96;
        }
        else if (confidenceLevel == 99) { 
            z = 2.58;
        }
        else {
            z = 1.645;
        }

        computeAverages(repeatedData, repetitions, iterations);

        computeVariances(repeatedData, repetitions, iterations, averages);

        for (int i = 0; i < iterations; i++) {
            int it = averages.get(i).getAccumulatedSamples();
            double mean = averages.get(i).getValue();
            double variance = variances.get(i);
            //System.out.println("variance = " + variance);
            double standardDeviation = Math.sqrt(variance);
            //System.out.println("std = " + standardDeviation);
            double error = (z * standardDeviation)/(Math.sqrt(repetitions));
            //System.out.println("error = " + error);
            double upper = mean + error;
            double lower = mean - error;
            upperConfidences.add(new DataPoint(it, upper));
            lowerConfidences.add(new DataPoint(it, lower));
        }

    }

    public void computeVariances(ArrayList<ArrayList<DataPoint>> repeatedData, int repetitions, int iterations, ArrayList<DataPoint> averages) {
        variances = new ArrayList<>();
        for (int i = 0; i < iterations; i++) {
            double var = 0.0;
            for (int r = 0; r < repetitions; r++) {
                double value = repeatedData.get(r).get(i).getValue();
                var += Math.pow((value - averages.get(i).getValue()),2);
            }
            var = var/repetitions;
            variances.add(var);
        }
    }


    public void computeModeMinMax(ArrayList<ArrayList<DataPoint>> repeatedData, int repetitions, int iterations) {
        minima = new ArrayList<>();
        maxima = new ArrayList<>();
        modi = new ArrayList<>();

        for (int i = 0; i < iterations; i++) {
            double min = Double.MAX_VALUE;
            double max = Double.MIN_VALUE;
            HashMap<Double, Integer> counts = new HashMap<>();
            for (int r = 0; r < repetitions; r++) {
                double value = repeatedData.get(r).get(i).getValue();
                min = Double.min(value, min);
                max = Double.max(value, max);
                if (counts.containsKey(value)) 
                    counts.put(value, counts.get(value)+1);
                else
                    counts.put(value, 1);
            }

            double mode = 0.0;
            int count = 0;
            for (double key : counts.keySet()) {
                int n = counts.get(key);
                if (n > count) {
                    count = n;
                    mode = key;
                }
            }
            
            minima.add(min);
            maxima.add(max);
            modi.add(mode);
        }



    }



    public String averagesToString() {
        return dataPointsToString(this.averages);
    }

    public String upperConfidencesToString() {
        return dataPointsToString(this.upperConfidences);
    }

    public String lowerConfidencesToString() { 
        return dataPointsToString(this.lowerConfidences);
    }

    public String dataPointsToString(ArrayList<DataPoint> data) {
        String str = "";
        for (int i = 0; i < data.size(); i++) {
            DataPoint d = data.get(i);
            str += "(" + d.getAccumulatedSamples() + ", " + d.getValue() + ")\n";
        }
        return str;
    }

    public String dataPointsToTikzString(ArrayList<DataPoint> data, String color, String name) {
        String str = "";
        str += "\\addplot[color="+color+", name path="+name+"] coordinates {\n";
        str += dataPointsToString(data);
        str += "\n};\n\n";
        return str;
    }



    public String processAverageToString(ArrayList<ArrayList<DataPoint>> repeatedData, int repetitions, int iterations) {
        String str = "";
        for (int i = 0; i < iterations; i++) {
            double sum = 0.0;
            int observations = 0;
            for (int r = 0; r < repetitions; r++) {
                DataPoint d = repeatedData.get(r).get(i);
                double value = d.getValue();
                int obs = d.getAccumulatedSamples();
                sum += value;
                observations += obs;
            }
            double average = sum / repetitions;
            observations = observations / repetitions;
            str += "(" + observations +", " + average + ")\n";
        }
        return str;
    }

    public String processMinToString(ArrayList<ArrayList<DataPoint>> repeatedData, int repetitions, int iterations) {
        String str = "";
        for (int i = 0; i < iterations; i++) {
            double min = 2.0;
            int observations = 0;
            for (int r = 0; r < repetitions; r++) {
                DataPoint d = repeatedData.get(r).get(i);
                double value = d.getValue();
                int obs = d.getAccumulatedSamples();
                min = Double.min(min, value);
                observations += obs;
            }
            observations = observations / repetitions;
            str += "(" + observations + ", " + min + ")\n";
        }
        return str;
    }

    public String processMaxToString(ArrayList<ArrayList<DataPoint>> repeatedData, int repetitions, int iterations) {
        String str = "";
        for (int i = 0; i < iterations; i++) {
            double max = -1.0;
            int observations = 0;
            for (int r = 0; r < repetitions; r++) {
                DataPoint d = repeatedData.get(r).get(i);
                double value = d.getValue();
                int obs = d.getAccumulatedSamples();
                max = Double.max(max, value);
                observations += obs;
            }
            observations = observations / repetitions;
            str += "(" + observations + ", " + max + ")\n";
        }
        return str;
    }


    public String addAxis(String ylabel, int xmax, int ymax) {

        String str = "";
        if (this.fullDocument) {
            str += "\\documentclass{standalone}\n" +
            "\\usepackage[dvipsnames]{xcolor}\n" +
            "\\usepackage{pgfplots}\n" +
            "\\pgfplotsset{compat=newest}\n" +
            "\\usepgfplotslibrary{fillbetween}\n" +
            "\\begin{document}\n";
        }

        str += "\\begin{tikzpicture}\n\\begin{axis}[xlabel=Samples processed,\nlegend pos=north east,\nxmin=0,\nymin=0,";
        str += "ylabel="+ylabel+",\n";
        str += "xmax="+ xmax +",\nymax="+ymax+",\nrestrict y to domain=-1000:1000]\n\n";

        plots = str + plots;

        return str;

    }

    public String addOpt(int xmax, double opt) {
        String str = "";
        str += "\\addplot[dashed,color=black] coordinates {\n";
        str += "(0," + opt + ")\n";
        str += "(" + xmax +"," + opt + ")\n};\n\n"; 
        plots += str;    
        return str;
    }

    public String sulOpt(ArrayList<ArrayList<DataPoint>> repeatedData, int repetitions, int iterations) {
        int xmax = repeatedData.get(0).get(iterations-1).getAccumulatedSamples();
        String str = "";
        str += "\\addplot[dashed,color=black] coordinates {\n";
        str += "(0," + this.optimum + ")\n";
        str += "(" + xmax +"," + this.optimum + ")\n};\n\n";     
        return str;

    }

    public String addLegend(String[] names) {
        String str = "";
        str += "\\legend{";
        for (int i = 0; i < names.length; i++) {
            str += names[i]+",";
            str += " , , , ";
        }
        str += "};\n\\end{axis}\n\\end{tikzpicture}";
        if (this.fullDocument) {
            str += "\n\\end{document}\n";
        }

        plots += str;
        return plots;
    }


    public String printPlots() {
        System.out.println(this.plots);
        return plots;
    }


    public String addLinePlot(int color, String name, ArrayList<ArrayList<DataPoint>> repeatedData, int repetitions, int iterations, int confidenceLevel) {
        if (color >= colors.length) {
            System.out.println("Error in DataProcessor: color index > number of colors.");
            System.exit(1);
        }
        
        this.averages.clear();
        this.lowerConfidences.clear();
        this.upperConfidences.clear();
        computeConfidenceIntervals(repeatedData, repetitions, iterations, confidenceLevel);

        String str = "";

        str += "% averages\n";
        str += "\\addplot[color="+colors[color]+", name path="+name+"avg] coordinates {\n";
        str += averagesToString();
        str += "\n};\n\n";

        str += "% min confidence\n";
        str += "\\addplot[color="+colors[color]+"!10, name path="+name+"min] coordinates {\n";
        str += lowerConfidencesToString();
        str += "\n};\n";

        str += "% max confidence\n";
        str += "\\addplot[color="+colors[color]+"!10, name path="+name+"max] coordinates {\n";
        str += upperConfidencesToString();
        str += "\n};\n";

        str += "% fill\n";
        str += "\\addplot["+colors[color]+"!10] fill between[of="+name+"min and "+name+"max];\n\n\n";

        plots += str;

        return str;

    }

    public void dumpRawData(String directoryPath, String name, ArrayList<DataPoint> dataPoints, Experiment experiment){
        try {
            //System.out.println("DEBUG \t\t (max r,max e) = (" + repetitions + ", " + iterations + ")");
            DataPoint previous = null;

            String path = directoryPath + name +".csv";
            if (Files.exists(Paths.get(path)))
                System.out.println("File" + path + "already exists");

            FileWriter writer = new FileWriter(path, false);

            writer.write("Accumulated Samples,Performance,Estimated Performance,Average Distance,Lower Bound,Upper Bound,Episode,Estimated Optimistic Performance,Optimistic Performance");
            writer.write(System.getProperty( "line.separator" ));

            for (DataPoint entry : dataPoints) {
                if (!entry.equals(previous)) {
                    String row = entry.getAccumulatedSamples() + ","
                            + entry.getValue() + ","
                            + entry.getEstimatedValue() + ","
                            + entry.getDistance() + ","
                            + entry.getLowerBound() + ","
                            + entry.getUpperBound() + ","
                            + entry.getEpisode() + ","
                            + entry.getOptimisticEstimatedValue() + ","
                            + entry.getOptimisticValue();
                    writer.write(row + System.getProperty("line.separator"));
                    previous = entry;
                }
            }
            writer.close();
        } catch (IOException e) {
            System.out.println(e);
        }
    }


    public String addScatterPlot(int color, String name, ArrayList<ArrayList<DataPoint>> repeatedData, int repetitions, int iterations) {
        computeModeMinMax(repeatedData, repetitions, iterations);
        String str = "";

        str += "\\addplot[only marks,color="+colors[color]+"] coordinates {\n";
        // TODO

        plots  += str;
        return str;
    }


    public String getTikzGraph() {
        System.out.println(this.plots);
        return this.plots;
    }


    public String getTikzGraph(String pathPrefix) {
        String path = pathPrefix + ".tex";
        try {    
            FileWriter fw = new FileWriter(path);    
            fw.write(this.plots);    
            fw.close();    
        } catch(Exception e) {
            System.out.println(e);
        }       
         
        System.out.println("% exported to   " + path + "\n%------------\n\n\n");
//        System.out.println(this.plots);
        return this.plots;
    }


}
