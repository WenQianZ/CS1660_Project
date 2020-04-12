import java.io.IOException;
import java.util.StringTokenizer;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.lib.input.*;
import org.apache.hadoop.mapreduce.lib.output.*;
import java.util.HashMap;
import org.apache.hadoop.conf.Configuration;

public class InvertedIndexJob{
  public static class WordDocMapper extends Mapper<LongWritable, Text, Text, Text>
  {
    private Text word = new Text();
    @Override
    public void map(LongWritable key, Text value, Context context)
      throws IOException, InterruptedException
    {
        String fileName = ((FileSplit) context.getInputSplit()).getPath().getName();
        String line = value.toString().toLowerCase().replaceAll("[^\\x00-\\x7F]", "");
        StringTokenizer tokenizer = new StringTokenizer(line, " \r\t\n\f\"?!<>{}[]()'*/,.:;~_-+=^%$@&#");
        while (tokenizer.hasMoreTokens())
        {
          word.set(tokenizer.nextToken());
          context.write(word, new Text(fileName));
          /*
              'word1' doc1
          */
        }
    }
  }

  //key is the term value is fileName
  public static class WordDocReducer extends Reducer<Text,Text,Text,Text>
  {
    @Override
    public void reduce(Text key, Iterable<Text> values, Context context)
      throws IOException, InterruptedException
    {
        HashMap<String,Integer> map = new HashMap<String,Integer>();
        for (Text value : values) {
          String s = value.toString();
          if(map.containsKey(s)) {
            map.put(s, map.get(s) + 1);
          } else {
            map.put(s, 1);
          }
        }
        context.write(key, new Text(map.toString()));
    }
  }

  public static void main(String[] args)
    throws IOException, ClassNotFoundException, InterruptedException{
    if (args.length != 3) {
        System.err.println("Usage: invertedindex <in> <out> <merge>");
        System.exit(-1);
    }
    Path srcPath = new Path(args[1]);
    Job job = new Job();
    job.setJarByClass(InvertedIndexJob.class);
    job.setJobName("Inverted Index");
    job.setMapperClass(WordDocMapper.class);
    job.setReducerClass(WordDocReducer.class);
    job.setOutputKeyClass(Text.class);
    job.setOutputValueClass(Text.class);
    FileInputFormat.addInputPath(job, new Path(args[0]));
    FileOutputFormat.setOutputPath(job, srcPath);
    job.waitForCompletion(true);

    Path dstPath = new Path(args[2]);
    Configuration conf = new Configuration();
    try {
      FileSystem hdfs = srcPath.getFileSystem(conf);
      hdfs.delete(new Path(args[1] + "/_SUCCESS"), false);
      FileUtil.copyMerge(hdfs, srcPath, hdfs, dstPath, false, conf, null);
    } catch (IOException e) { }
    //https://stackoverflow.com/questions/12911798/hadoop-how-can-i-merge-reducer-outputs-to-a-single-file/12922262
  }
}
