package code;

import java.awt.*;
import java.util.ArrayList;
import java.awt.event.ActionListener;
import java.io.*;
import java.awt.event.ActionEvent;
import javax.swing.*;

import org.apache.commons.codec.Charsets;
import org.apache.http.HttpResponse;
import org.apache.http.client.*;
import org.apache.http.client.methods.*;
import org.apache.http.entity.*;
import org.apache.http.impl.client.HttpClients;
import com.google.common.io.ByteSource;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


public class UI implements Runnable{
	private static String access_token = "Bearer ";
	private String bucket = "dataproc-staging-us-west1-301399383302-ixrbicuk";
	private String project_id = "pure-quasar-273515";
	private String region = "us-west1";
	private String cluster_name = "cluster-f974-1";
	private String job_id = "";
	private JFrame _frame; 
	private JLabel label_note = new JLabel();
	private JPanel panel_l_note = new JPanel();
	private JPanel panel_choose_file = new JPanel();
	private JButton b_choose_file = new JButton("Choose Files");
	private ArrayList<File> files = new ArrayList<File>();
	private String file_name = new String();
	private JLabel label_file_name = new JLabel();
	private JPanel panel_b_load = new JPanel();
	private JButton b_load_engine = new JButton("Load Engine");
	private JPanel panel_b_action = new JPanel();
	private JButton b_search_term = new JButton("Search for Term");
	private JTextField tf = new JTextField();
	private JButton b_search =  new JButton("Search");
	private JPanel panel_tf = new JPanel();
	private String term = "";
	private JPanel panel_search_result = new JPanel();
	private JLabel label_search_result = new JLabel();
	public UI() {
	}
	 
	public static void main(String[] args) {
		javax.swing.SwingUtilities.invokeLater(new UI());
		if(args.length < 1) {
			System.err.println("Need access token");
			System.exit(0);
		}
		access_token += args[0];
	}
	
	@Override
	public void run() {
		_frame = new JFrame("Wenqian Search Engine");
		label_note.setOpaque(true);
		label_note.setFont(new Font("Consolas", Font.BOLD, 28));
		label_note.setHorizontalAlignment(JLabel.CENTER);
		panel_l_note.add(label_note);
		panel_l_note.setLayout(new GridLayout());
		_frame.add(panel_l_note);
		_frame.getContentPane().setLayout(new BoxLayout(_frame.getContentPane(), BoxLayout.Y_AXIS));
		main_page();
		_frame.setPreferredSize(new Dimension(800, 800));
		_frame.setBackground(Color.white);
		_frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		_frame.pack();
		_frame.setVisible(true);
		
	}
	
	public void main_page() {
		label_note.setText("Load My Engine");
		b_choose_file.setFont(new Font("Consolas", Font.BOLD, 20));
		b_choose_file.setPreferredSize(new Dimension(200, 50));
		b_choose_file.setBackground(Color.LIGHT_GRAY);
		b_choose_file.setFocusPainted(false);
		b_choose_file.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				JFileChooser chooser = new JFileChooser();
				chooser.setMultiSelectionEnabled(true);
				int returnVal = chooser.showOpenDialog(null);
		        if(returnVal == JFileChooser.APPROVE_OPTION) {
		            for(File f: chooser.getSelectedFiles()) {
		            	if(files.contains(f)) {
		            		System.out.println("Already exist:" + f.getName());
		            	} else {
		            		files.add(f);
		            		file_name = file_name + f.getName() +"&#09;&#09;" ;
		            		System.out.println("Added file:" + f.getName());
		            		label_file_name.setText("<html>"+file_name+"</html>");
		            	}
		            }
		            load_engine();
		        }
				
			}
		});
		label_file_name.setFont(new Font("Consolas", Font.BOLD, 14));
		panel_choose_file.add(b_choose_file);
		panel_choose_file.add(label_file_name);
		panel_choose_file.add(Box.createVerticalGlue(), BorderLayout.NORTH);
		panel_choose_file.setLayout(new BoxLayout(panel_choose_file, BoxLayout.Y_AXIS));
		_frame.add(panel_choose_file);

	}
	public String get_content(HttpResponse r) {
		try {
			InputStream in = r.getEntity().getContent();
			ByteSource byteSource = new ByteSource() {
				@Override
		        public InputStream openStream() throws IOException {
		            return in;
		        }
		    };

		    return byteSource.asCharSource(Charsets.UTF_8).read();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "";
		
	}
	//POST https://storage.googleapis.com/upload/storage/v1/b/bucket/o
	public void upload(){
		try {
			for(File f: files) {
				
				HttpClient client = HttpClients.createDefault();
				HttpPost httppost = new HttpPost("https://storage.googleapis.com/upload/storage/v1/b/"+
							bucket + "/o?uploadType=media&name=data/" + f.getName());
				httppost.addHeader("Authorization", access_token);
	            httppost.setEntity(new FileEntity(f)) ;
	            System.out.println(client.execute(httppost));
			}    
		} catch(IOException e) {
			System.err.println("Fail to upload files");
			e.printStackTrace();
			remove_files();
			System.exit(0);
		}
		
	}
	//percent encoding: %2F = /
	public void remove_files() {
		try {
			for(File f: files) {
			
				HttpClient client = HttpClients.createDefault();
				HttpDelete httpdelete = new HttpDelete("https://storage.googleapis.com/upload/storage/v1/b/"+
							bucket + "/o/data%2F" + f.getName());
				httpdelete.addHeader("Authorization", access_token);
	            System.out.println(client.execute(httpdelete));
			}
		} catch(IOException e) {}
		
	}
	
	public void submit_job(){
		try {
			String request_body = get_request_body("gs://"+bucket+"/data", "gs://"+bucket+"/output", "gs://"+bucket+"/output.txt","gs://"+bucket+"/JAR/invertedindex.jar");
			HttpClient client = HttpClients.createDefault();
			HttpPost httppost = new HttpPost("https://dataproc.googleapis.com/v1/projects/"+ project_id+"/regions/" +region +"/jobs:submit");
			httppost.addHeader("Authorization", access_token);
			httppost.setEntity(new StringEntity(request_body));
			HttpResponse r = client.execute(httppost);
			JsonObject jsonObject = new JsonParser().parse(get_content(r)).getAsJsonObject();
			job_id = jsonObject.get("jobUuid").getAsString();
			System.out.println("job id: " + job_id);
		} catch (IOException e) {
			System.err.println("Fail to submit job");
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(0);
		}
	}
	// get job
//	https://dataproc.googleapis.com/v1/projects/pure-quasar-273515/regions/us-west1/jobs/a7207413-fb7c-46ad-812f-9fae5ab8c222
	public boolean job_done() {
        try {
        	HttpClient client = HttpClients.createDefault();
    		HttpGet httpget = new HttpGet("https://dataproc.googleapis.com/v1/projects/" + project_id + "/regions/" + region + "/jobs/" + job_id);
    		httpget.addHeader("Authorization", access_token);
			HttpResponse r = client.execute(httpget);
			JsonObject jsonObject = new JsonParser().parse(get_content(r)).getAsJsonObject();
			job_id = jsonObject.get("jobUuid").getAsString();
			return jsonObject.has("done");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		return false;
	}
	public void wait_job_to_finish() {
		while(true) {
			try {
				//wait for 10 second so its not constantly running
				Thread.sleep(10000);
				if(job_done()) {
					break;
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	}
	
	/* job submit:
	 *	https://cloud.google.com/dataproc/docs/reference/rest/v1/projects.regions.jobs/submit?apix_params=%7B%22projectId%22%3A%22pure-quasar-273515%22%2C%22region%22%3A%22us-west1%22%2C%22resource%22%3A%7B%22job%22%3A%7B%22hadoopJob%22%3A%7B%22args%22%3A%5B%22gs%3A%2F%2Fdataproc-staging-us-west1-301399383302-ixrbicuk%2Fdata%22%2C%22gs%3A%2F%2Fdataproc-staging-us-west1-301399383302-ixrbicuk%2Foutput%22%5D%2C%22mainClass%22%3A%22InvertedIndexJob%22%2C%22jarFileUris%22%3A%5B%22gs%3A%2F%2Fdataproc-staging-us-west1-301399383302-ixrbicuk%2FJAR%2Finvertedindex.jar%22%5D%7D%2C%22placement%22%3A%7B%22clusterName%22%3A%22cluster-f974-1%22%7D%7D%7D%7D
	 * job object:
	 * 	https://cloud.google.com/dataproc/docs/reference/rest/v1/projects.regions.jobs#Job
	 * Hadoop job:
	 * 	https://cloud.google.com/dataproc/docs/reference/rest/v1/HadoopJob
	 * data: gs://dataproc-staging-us-west1-301399383302-ixrbicuk/data		
	 * output: gs://dataproc-staging-us-west1-301399383302-ixrbicuk/output
	 * jarFileUris: gs://dataproc-staging-us-west1-301399383302-ixrbicuk/JAR/invertedindex.jar
	 * job object format
     *				  "job": {
     *				    "hadoopJob": {
     *				      "args": [
     *				        "gs://dataproc-staging-us-west1-301399383302-ixrbicuk/data",
     *				        "gs://dataproc-staging-us-west1-301399383302-ixrbicuk/output"
     *				      ],
     *				      "mainClass": "InvertedIndexJob",
     *				      "jarFileUris": [
     *				        "gs://dataproc-staging-us-west1-301399383302-ixrbicuk/JAR/invertedindex.jar"
     *				      ]
     *				    },
     *				    "placement": {
     *				      "clusterName": "cluster-f974-1"
     *				    }
     *				  }
     *				}
	 */
	public String get_request_body(String data, String output, String output_text, String jarFileUris) {
		return "{\"job\": {"
							+ "\"hadoopJob\": {"
								+ "\"args\": ["
									+ "\""+ data + "\","
									+ "\"" + output + "\","
									+ "\"" + output_text + "\""
								+ "],"
								+ "\"mainClass\": \"InvertedIndexJob\","
								+ "\"jarFileUris\": ["
									+ "\"" + jarFileUris  + "\""
								+"]"
							+ "},"
							+ "\"placement\": {"
								+ "\"clusterName\":" + "\"" + cluster_name + "\""
							+ "}"
						+ "}"
					+ "}";
	}
	
	
	public void load_engine() {
		b_load_engine.setFont(new Font("Consolas", Font.BOLD, 20));
		b_load_engine.setPreferredSize(new Dimension(200, 50));
		b_load_engine.setBackground(Color.LIGHT_GRAY);
		b_load_engine.setFocusPainted(false);
		b_load_engine.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				System.out.println("Uploading files");
				upload();
				System.out.println("Submitting job");
				submit_job();
				System.out.println("Waiting job to finish");
				wait_job_to_finish();
				System.out.println("Job is finished");
				job_finish();
			}
		});
		panel_b_load.add(b_load_engine);
		panel_b_load.add(Box.createVerticalGlue(), BorderLayout.NORTH);
		panel_b_load.setLayout(new BoxLayout(panel_b_load, BoxLayout.Y_AXIS));
		_frame.add(panel_b_load);
		
	}
	
	public void job_finish() {
		label_note.setText("<html> &#09;&#09;&#09; Engine was loaded <br/>"
				+ " &#09;&#09;&#09;&#09; &emsp;&emsp;and <br/> Inverted indicies were constructed successfully! "
				+ "<br/> <br/>"
				+ "&#09;&#09;&#09;Please Select Action</html>");
		panel_choose_file.setVisible(false);
		panel_b_load.setVisible(false);
		b_search_term.setFont(new Font("Consolas", Font.BOLD, 20));
		b_search_term.setPreferredSize(new Dimension(200, 50));
		b_search_term.setBackground(Color.LIGHT_GRAY);
		b_search_term.setFocusPainted(false);
		b_search_term.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				search_term();
				
			}
				
			
		});
		panel_b_action.add(b_search_term);
		panel_b_action.add(Box.createVerticalGlue(), BorderLayout.NORTH);
		panel_b_action.setLayout(new BoxLayout(panel_b_action, BoxLayout.Y_AXIS));
		_frame.add(panel_b_action);
		
	}
	
	public void search_term() {
		label_note.setText("Enter Your Search Term");
		panel_b_action.setVisible(false);
		b_search.setFont(new Font("Consolas", Font.BOLD, 20));
		b_search.setPreferredSize(new Dimension(200, 50));
		b_search.setBackground(Color.LIGHT_GRAY);
		b_search.setFocusPainted(false);
		b_search.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				term = tf.getText().toLowerCase();
				if(term.compareTo("")!=0 ) {
					search_result();
				}
			}	
			
		});
		tf.setFont(new Font("Consolas", Font.BOLD, 14));
		tf.setPreferredSize(new Dimension(400, 30));
		panel_tf.add(tf);
		panel_tf.add(b_search);
		_frame.add(panel_tf);
	}
	public void search_result() {
		try {
			HttpClient client = HttpClients.createDefault();
			HttpGet httpget = new HttpGet("https://storage.googleapis.com/storage/v1/b/" + bucket + "/o/output.txt?alt=media");
			httpget.addHeader("Authorization", access_token);
			HttpResponse r = client.execute(httpget);
			String[] texts = get_content(r).split("\n");
			String found = "";
			long start = System.nanoTime();
			Double time = 0.0;
			for(String text: texts) {
				String[] s = text.split("\t");
				if(s[0].compareTo(term) == 0) {
					found = s[1].substring(1, s[1].length()-1);
					panel_search_result.setVisible(true);
					time = ((System.nanoTime() - start)/(1000000.0));
					System.out.println("time elapsed: " + time + "ms");
					label_note.setText("<html> You searched for the term: <i>" + term +"</i>"
										+ "<br/> Your search was executed in <i>" + time + "</i> ns</html>");
					String[] collections = found.split(", ");
					String result = "<tr><th>Document Name</th> <th>Frequencies</th></tr>";
					
					for (String collection: collections) {
						//info[0] = document   info[1] = frequency
						String[] info = collection.split("=");
						result += "<tr>" + "<td>" + info[0] + "</td>"+ "<td>" + info[1] + "</td>"+"</tr>";
					}
					label_search_result.setOpaque(true);
					label_search_result.setText("<html><style> table, th, td { border: 1px solid black; border-collapse: collapse;}th, td {padding: 5px;text-align: left;}\r\n" + 
							"</style> <table>" + result +"</table></html>");
					label_search_result.setFont(new Font("Consolas", Font.BOLD, 18));
					label_search_result.setPreferredSize(new Dimension(1200, 300));
					label_search_result.setHorizontalAlignment(JLabel.CENTER);
					panel_search_result.add(label_search_result);
					panel_search_result.setBackground(Color.blue);
					panel_search_result.setLayout(new GridLayout());
					_frame.add(panel_search_result);
					return;
				}
			}
			time = ((System.nanoTime() - start)/(1000000.0));
			System.out.println("time elapsed: " + time + "ms");
			label_note.setText("<html> You searched for the term: <i>" + term +"</i>"
								+ "<br/> Your search was executed in <i>" + time + "</i> ns</html>");
			
			label_search_result.setText("No result found.");
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}



