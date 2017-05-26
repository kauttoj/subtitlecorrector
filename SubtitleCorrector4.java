import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLDecoder;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import org.languagetool.*;
import org.languagetool.language.*;
import org.languagetool.rules.RuleMatch;
import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.input.BOMInputStream;
//import org.apache.commons.net.io.*;


/*
 *  TODO:
	- Aikatunnisteiden älykäs korjaus (tunnistaa virheellisen tekstin, ei poista hyviä)
	- rivien yli jatkuvat sulut


 */
public class SubtitleCorrector extends Start {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * @param args
	 */
	final int MAX_LINESIZE = 40,
			TARGET_DIFFERENCE = 2; // TARGET_DIFFERENCE == line1_length-line2_length;
	final double LINE2_MULTIPLIER = 0.87f,
			LINE1_MULTIPLIER=0.97f;
	final double MAX_DURATION = 6.0;

	final double mTimeOffset = 0.095f;
	String destFileNm = "";
	
	
	//langTool.activateDefaultPatternRules();

	boolean mStatus = false;		
	
	boolean alreadyFixed = false;
	protected String filename = "";
	private final static String ISO_ENCODING = "ISO-8859-1";
	private final static String UTF8_ENCODING = "UTF-8";
	private final static int UTF8_BOM_LENGTH = 3;


	LinkedList<SingleLine> data = new LinkedList<SingleLine>();
	
	public BadLines badlines = new BadLines();

	public ErrorTypes errors = new ErrorTypes();

	public SubtitleCorrector() {

		
	}
	
	
	public class Indices {
		int alku=-1,loppu=-1;
		String str = " ";
	}
	
	
	public class BadLines {		
		
		ArrayList lista = new ArrayList();
		int koko = 0;
		void add(int i) {
			lista.add(i);
			koko = lista.size();
		}
		public String giveString() {
			if (koko>0) {
			String a = "";
			for (int i=0;i<koko;i++)
				a = a + lista.get(i) + ", ";
			return a.substring(0,a.length()-2);
			}
			else return "";				
		}
	}

	public class ErrorTypes {
		public int IndexErrors=0;
		public int HearingImpairedErrors=0;
		public int TimingErrors=0;
		public int LineChanges=0;
		public int BrokenBlock = 0;
		public String giveString() {
			return IndexErrors + " (index), " + HearingImpairedErrors + " (HI), " + TimingErrors + " (timing), " + LineChanges + " (lines), " + BrokenBlock + " (blocks)";
		}
	}

	public static class Pair {

	    public final int first;
	    public final int second;

	    public Pair(int first,int second) {
	        this.first = first;
	        this.second = second;
	    }

	}

	public class TimeHolder {
		double totaltime,seconds;
		int hours,minutes;

		TimeHolder() {
			totaltime=0;
			seconds=0;
			minutes=0;
			hours=0;
		}
		int giveHours() {
			return hours;
		}
		int giveMinutes() {
			return minutes;				
		}
		double giveSeconds() {
			return seconds;
		}

		void compute_total() {
			totaltime = hours*60*60 + minutes*60 + seconds;
		}

		boolean parsetime(String ts) {
			//String atoms[] = ts.split("\\,");
			String a[] = ts.split(":");
			
			if (a.length>1) {
			
			String b[] = a[2].split(",");
			seconds = Double.parseDouble(b[0]) + Double.parseDouble(b[1])/1000; /* seconds */
			minutes = Integer.parseInt(a[1]); /* minutes */
			hours   = Integer.parseInt(a[0]); /* hours */
			compute_total();
			return true;
			}
			else
				return false;
		}
		
		public String toString() {
			return get2digit(hours,2) + ":" + get2digit(minutes,2) + ":" + strPre(seconds);
		}
		
		public boolean largerthan(TimeHolder time) {
			// TODO Auto-generated method stub
			if (this.totaltime>time.totaltime)
				return true;			
			else
				return false;
		}
		
		public boolean largerthan(TimeHolder time,double offset) {
			// TODO Auto-generated method stub
			if (this.totaltime > time.totaltime+offset)
				return true;			
			else
				return false;
		}		
		
		public void add(double a) {
			// TODO Auto-generated method stub
			int b=0;
			this.seconds += a;
			if (this.seconds>60) {
				b =  (int) Math.floor(this.seconds/60.0);
				this.minutes += b;
				this.seconds -= (double)b*60.0;
				b =  (int) Math.floor(this.minutes/60.0);
				this.hours += b;
				this.minutes -= b*60;				
			}
			compute_total();
			//return null;
		}
		public void copy(TimeHolder obj) {
			this.totaltime = obj.totaltime;
			this.seconds = obj.seconds;
			this.hours = obj.hours;
			this.minutes = obj.minutes;
			// TODO Auto-generated method stub

		}

	}
	// TODO Auto-generated method stub
	public class SingleLine {
		int rows;
		int index,real_index;
		int line1_length,line2_length;
		int times_entered;
		TimeHolder startTime,endTime;
		double duration;
		String line1,line2;
		SingleLine() {
			times_entered = 0;
			rows=0;
			index = -1;
			line1="";
			duration=-1;
			line1_length=0;
			line2_length=0;
			line2="";
			startTime = new TimeHolder();
			times_entered =0; 
			endTime = new TimeHolder();
		}

		String[] joiner(String str[]) {

			int a = 0;
			for (int i=0;i<str.length-1-a;i++) {
				if ( 		(str[i].toUpperCase()).equals("A") 
						|| (str[i].toUpperCase()).equals("THE") 
						|| (str[i].toUpperCase()).equals("AN") 
						|| (str[i].toUpperCase()).equals("I") 
						|| (str[i].toUpperCase()).equals("–") 
						|| (str[i].toUpperCase()).equals("-")
						) {										

					str[i] = str[i] + " " + str[i+1];
					for (int j=i+1;j<str.length-1;j++)
						str[j] = str[j+1];
					a++;
				}			
			}
			if (a>0) {
				String b[] = new String[str.length-a];
				for (int i=0;i<b.length;i++)
					b[i]=str[i];			
				return b;
			}
			else return str;
		}

		void clearLine() {
			line1="";
			line2="";
			line1_length=0;
			line2_length=0;
			rows = 1;
		}
		
		void distribute() {

			int pit = 0,ind=0,indd,pitt;
			double suhde,paras;

			if (rows==1) {
				if (line1_length>MAX_LINESIZE) {
					String str[] = joiner(line1.split(" "));					

					/* halutut ehdot:  	line1_length > line2_length*line1_multiplier
									line2_length > line2_length*line2_multiplier
					 */
					ind = str.length-1;
					pit = str[ind].length()+1;	
					indd=-1;
					pitt=0;
					suhde = Math.abs(line1_length-TARGET_DIFFERENCE-line2_length);
					paras=suhde;
					//found = false;
					while (ind > 0) {
						/*
					if ( line1_length - pit > Math.ceil((line2_length+pit)*line1_multiplier) 
						 && line2_length + pit > Math.ceil((line1_length-pit)*line2_multiplier)
					    ) {
						found = true;
						break;
					}
					else {
						 */

						suhde = Math.abs(line1_length-2*pit-TARGET_DIFFERENCE-line2_length);
						if (suhde<paras) {
							paras=suhde;
							pitt=pit;
							indd=ind;
						}
						ind--;
						pit += str[ind].length()+1;

					}
					if (indd>-1) {
						for (int i=str.length-1;i>indd-1;i--) {
							line2 = new String(str[i] + " " + line2);												
						}				
						line1 = line1.substring(0,line1.length()-pitt);
						rows=2;
						errors.LineChanges++;

					}
				}

			}
			else {

				if (noLines(line1,line2)) { 


					//boolean found = false;
					// rivi 1 liian lyhyt
					if (line1_length < Math.ceil((double)line2_length*LINE1_MULTIPLIER) ) {
						String str[] = joiner(line2.split(" "));					

						/* halutut ehdot:  	line1_length > line2_length*line1_multiplier
										line2_length > line2_length*line2_multiplier
						 */
						ind = 0;
						pit = str[ind].length()+1;
						pitt=0;
						indd=-1;
						suhde=Math.abs(line1_length-TARGET_DIFFERENCE-line2_length);
						paras=suhde;
						//found = false;
						while (ind < str.length-1) {	
							/*
						if ( line1_length + pit > Math.ceil((line2_length-pit)*line1_multiplier) 
							 && line2_length - pit > Math.ceil((line1_length+pit)*line2_multiplier)
						    ) {
							found = true;
							break;
						}
						else {
							 */
							suhde = Math.abs(line1_length+2*pit-TARGET_DIFFERENCE-line2_length);
							if (suhde<paras) {
								paras=suhde;
								pitt=pit;
								indd=ind;
							}
							ind++;
							pit += str[ind].length()+1;

						}
						if (indd>-1) {
							for (int i=0;i<indd+1;i++) {
								line1 = new String(line1 + " " + str[i]);												
							}				
							line2 = line2.substring(pitt);
							errors.LineChanges++;
						}


					}
					// rivi 2 liian lyhyt
					else if (line2_length < Math.ceil((double)line1_length*LINE2_MULTIPLIER) ) {
						String str[] = joiner(line1.split(" "));					

						/* halutut ehdot:  	line1_length > line2_length*line1_multiplier
										line2_length > line2_length*line2_multiplier
						 */
						ind = str.length-1;
						pit = str[ind].length()+1;	
						indd=-1;
						pitt=0;
						suhde = Math.abs(line1_length-TARGET_DIFFERENCE-line2_length);
						paras=suhde;
						//found = false;
						while (ind > 0) {
							/*
						if ( line1_length - pit > Math.ceil((line2_length+pit)*line1_multiplier) 
							 && line2_length + pit > Math.ceil((line1_length-pit)*line2_multiplier)
						    ) {
							found = true;
							break;
						}
						else {
							 */

							suhde = Math.abs(line1_length-2*pit-TARGET_DIFFERENCE-line2_length);
							if (suhde<paras) {
								paras=suhde;
								pitt=pit;
								indd=ind;
							}
							ind--;
							pit += str[ind].length()+1;

						}
						if (indd>-1) {
							for (int i=str.length-1;i>indd-1;i--) {
								line2 = new String(str[i] + " " + line2);												
							}				
							line1 = line1.substring(0,line1.length()-pitt);
							errors.LineChanges++;
						}

					}
				}
			}
		}


		void addindex(int a,int b) {
			index = a;
			real_index = b;
		}

		void update() {
			duration = endTime.totaltime - startTime.totaltime;
		}



		void fix_lines() {
			
			line1_length=line1.length();
			line2_length=line2.length();		
			
			// POISTAA "KUKA PUHUU NYT" JUTUT			

			int end ;
			int start ;
			//int first_start=start;

			//LinkedList<String> nimet = new LinkedList<String>();

			String old_name=null;
			boolean multiple_names=false;				

			StringBuilder str=new StringBuilder(line1 + " " + line2);
			Indices ind[] = new Indices[20];

			end = str.indexOf(":");
			start = end;
			int counter=0;
			int leikkauskohta = line1_length;
			while (end>-1) {

				if (!(  
						(start>0 && (Character.isDigit(str.charAt(start-1)) || str.charAt(start-1)=='>')) &&
						(end<str.length()-1 && Character.isDigit(str.charAt(end+1))) )) 
				 {						
					while (true) {			
						if (start > 0 && str.charAt(start-1)!=' ' && str.charAt(start-1)!='>')
							start--;
						else break;					
					}
					while (start > 1 && Character.isUpperCase(str.charAt(start-2))) {
						start = start - 1;				
						while (true) {			
							if (start > 0 && Character.isUpperCase(str.charAt(start-1)))
								start--;
							else break;
						}
					}				
					//start = Math.max(start,0);
					String str1 = str.substring(start, end);
					String str2 = str.substring(start, end).toUpperCase();
					
					if (isAlmostEqual(str1,str2)) {
						//if (str.length()-1>end && str.charAt(end+1)==' ' )
						end++;									
						if (multiple_names==false && old_name!=null) { 
							String c = str.substring(start, end-1);
							if (!old_name.equalsIgnoreCase(c))
								multiple_names=true;
						}
						else old_name = new String(str.substring(start, end-1));

						ind[counter] = new Indices();
						ind[counter].alku=start;
						ind[counter].loppu=end;
						ind[counter].str = str.substring(start, end).toString();
						counter++;
						errors.HearingImpairedErrors++;
						
					}
				}
				end = str.indexOf(":",end+1);
				start = end;		
				
			}

			String[] a = {"",""};
			a[0]=line1;
			a[1]=line2;
			ReplaceComments(a,str,ind,counter,multiple_names,rows,leikkauskohta);
			
						
			
			
			
			
			
			line1=a[0].trim();
			line2=a[1].trim();
			line1_length=line1.length();
			line2_length=line2.length();			

			if (rows > 1) {
				if (line2_length==0)
					rows=1;
				if (line1_length==0 && line2_length>0) {		
					line1 = line2;
					line1_length=line2_length;
					rows = 1;
					errors.LineChanges++;
				}
			}	

			if (rows > 1 && noLines(line1,line2)) {

				if (line1_length + line2_length < MAX_LINESIZE) {
					/*
					if (line1.charAt(line1.length()-1)==' ' || line2.charAt(0) == ' ')
						line1 = line1 + line2;
					else
					 */
					line1 = line1 + " " + line2;
					line1_length = line1_length + line2_length + 1;
					rows = 1;
					errors.LineChanges++;
				}

			}

			//if (rows > 1) {
			distribute();
			line1_length=line1.length();
			line2_length=line2.length();
			//}


		}

		boolean isAlmostEqual(String str1,String str2) {
			
			double hit = 0.0;
			for (int i=0;i<str1.length();i++) {
				if (str1.charAt(i) == str2.charAt(i))
					hit = hit + 1.0;
			}
			
			if ( hit/(double)str1.length() > 0.5)
				return true;
			else
				return false;
			
			
		}
		
		
		void ReplaceComments(String[] a,StringBuilder str,Indices[] ind,int counter,boolean multiple_names,int rows,int leikkauskohta) {
			int h=0;
			if (counter>0) {
				for (int k=counter-1;k>-1;k--) {
					/*
					if (ind[k].loppu<=line1_length) {
					*/
						if (multiple_names) {							
							str.replace(ind[k].alku, ind[k].loppu,"\u002d");
							if ( ind[k].loppu <= line1_length)								
								leikkauskohta-=ind[k].str.length()-1;
							//h=h - (ind[k].loppu+ind[k].alku)+1;
						}
						else {
							str.replace(ind[k].alku, ind[k].loppu,"");
							//leikkauskohta-=ind[k].loppu-ind[k].alku;
							//h=ind[k].loppu-ind[k].alku;
							if ( ind[k].loppu <= line1_length)								
								leikkauskohta-=ind[k].str.length();
							else {
								rows = 1;
								a[1]="";
								leikkauskohta = str.length();
							}
								
						}						
						
						/*
					}
					else {
						if (multiple_names) {
							
						
							str.replace(ind[k].alku-h, ind[k].loppu-h,"–");
							h+=ind[k].loppu-ind[k].alku+1;
						}
						else {
							str.replace(ind[k].alku-h, ind[k].loppu-h,"");
							h+=ind[k].loppu-ind[k].alku;
						}
					}
					*/
				}					
			}	
			
			if (rows==1)
				a[0]=str.substring(0,leikkauskohta);			
			else {
				a[1]=str.substring(leikkauskohta+1);
				a[0]=str.substring(0,leikkauskohta);
			}
		}
		
		boolean noLines(String line1,String line2) {
			String str1[] = line1.split(" ");
			String str2[] = line2.split(" ");									
			if ( 	(str1[0].toUpperCase()).equals("–") 
					|| (str1[0].toUpperCase()).equals("-")  
					|| (str2[0].toUpperCase()).equals("–") 
					|| (str2[0].toUpperCase()).equals("-") 
					)
				return false;
			if (str1[0].length()>1 && str2[0].length()>1) {
				if ( 	(str1[0].substring(0,1)).equals("–") 
						|| (str1[0].substring(0,1)).equals("-") 
						|| (str2[0].substring(0,1)).equals("–") 
						|| (str2[0].substring(0,1)).equals("-")
						)
					return false;
			}
			return true;
		}

		boolean addtime(String a) {
			boolean res=false;
			if (times_entered == 0) {
				res = startTime.parsetime(a);
			}
			else {
				res = endTime.parsetime(a);
				if (res) 
					duration = endTime.totaltime - startTime.totaltime;
			}
			
			if (res) {
				times_entered++;
				return true;
			}
			else
				return false;
			

		}
		void addline(String a) {
			if (rows==0) {
				line1 = a;
				line1_length=line1.length();
			}
			else if (rows == 1) {
				line2 =  a;
				line2_length=line2.length();
			}
			else {
				line2 = line2 + " " + a;
				line2_length=line2.length();
			}
			rows++;
		}
	}

	public String ReadFile(URL url) throws IOException {

		/* INPUT: offset value: negative = less (-) ... positive = more (+). */
		//long delta = (22 * 1000L + 000); /* msec */


		/* INPUT: source & destination files */
		//String srcFileNm = "L:/DivX/Movies/TheMask.en.srt";

		String srcFileNm = url.getFile();
		srcFileNm = srcFileNm.replaceAll("\\+", "%2b");

		srcFileNm = URLDecoder.decode(srcFileNm, "utf-8");

		srcFileNm = new File(srcFileNm).getPath();


		//	int slashIndex = srcFileNm.lastIndexOf('\');
		int dotIndex = srcFileNm.lastIndexOf('.');

		filename = srcFileNm.substring(srcFileNm.lastIndexOf('\\')+1);
		
		String fileType = filename.substring(filename.length()-4);
		
		if (!fileType.toUpperCase().equals(".SRT"))
			throw new IOException("Not SRT file!");
		

		//String destFileNm/*,filetype*/;

		//filetype = srcFileNm.
		destFileNm = srcFileNm.substring(0,dotIndex) + "_FIXED" + srcFileNm.substring(dotIndex);


		/* offset algorithm: START */


		/* Open the file that is the first command line parameter */
		FileInputStream fstream = new FileInputStream(srcFileNm);
		DataInputStream in = new DataInputStream(fstream);				
		BufferedReader br = new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8));
		
		BOMInputStream bomIn = new BOMInputStream(new FileInputStream(srcFileNm), ByteOrderMark.UTF_8, ByteOrderMark.UTF_16BE,
		        ByteOrderMark.UTF_16LE, ByteOrderMark.UTF_32BE, ByteOrderMark.UTF_32LE);
		int firstNonBOMByte = bomIn.read(); // Skips BOM
		Boolean NeedStripping=false;
		if (bomIn.hasBOM()) {
		    // has a UTF-8 BOM
			NeedStripping =true;
		}
				/*
		final byte[] bytes = strLine.getBytes(ISO_ENCODING);
		if (isUTF8(bytes)) {
			strLine = printSkippedBomString(bytes);		
		}
		*/
		
		Scanner myscan = new Scanner(fstream,"UTF-8");
		myscan = myscan.useDelimiter("\n");
		
		//String testttt = myscan.nextLine();
		//BufferedReader br = new BufferedReader(new InputStreamReader(in,StandardCharsets.ISO_8859_1));
		String strLine;		

		/* Read File Line By Line */
		data.clear();
		int status = 0;
		Boolean NeedNormalization=false;
		int count=0;
		int added = 0;
		boolean broken = false;
		SingleLine tmp = new SingleLine();
		char apu1;
		while (myscan.hasNext()) {
			strLine = myscan.next();
			strLine = strLine.replaceAll("[\u0000-\u001f]", "");
			
			count++;														
			
			if (NeedStripping && count==1) {
				 strLine= strLine.substring(1) ;
			}
				//strLine = strLine.replaceAll("[\u0000-\u001f]", "");
			//}
				//strLine = Normalizer.normalize(strLine, Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");					
			
			if (strLine.isEmpty()) {
				
				//if (added == 220)
				//	System.out.println(added + "\n");
					
				
				if (status==2) {				
					added++;
					data.add(tmp);
				}
				if (broken) {
					errors.BrokenBlock++;
				}
				broken = false;
				status=0;
			}
			else if (status == 0 && broken==false) {
					
				tmp = new SingleLine();

				try {					
					tmp.addindex(Integer.parseInt(strLine.trim()),added);
					status = 1;
				}
				catch (NumberFormatException e) {
					broken=true;
				}

			}
			else if (status == 1  && broken==false) {			
				String[] atoms = strLine.split("-->");
				if ( atoms.length<2 || !tmp.addtime(atoms[0].trim()) || !tmp.addtime(atoms[1].trim()) ) 
					broken=true;
				else 
					status = 2;
			}
			else if (status == 2) {
				tmp.addline(strLine);
			}

		}
		if (status==2) {
			if (broken==false) {		
				added++;
				data.add(tmp);
			}
			else {
				errors.BrokenBlock++;
			}
		}

		mStatus = true;

		in.close();

		return filename;


	}
/*
	private static String printSkippedBomString(final byte[] bytes) throws UnsupportedEncodingException {
		int length = bytes.length - UTF8_BOM_LENGTH;
		byte[] barray = new byte[length];
		System.arraycopy(bytes, UTF8_BOM_LENGTH, barray, 0, barray.length);
		return new String(barray, ISO_ENCODING);
	}
*/
	/*
	private static void printUTF8String(final byte[] bytes) throws UnsupportedEncodingException {
		System.out.println(new String(bytes, UTF8_ENCODING));
	}
*/
	/*
	private static boolean isUTF8(byte[] bytes) {
		if ((bytes[0] & 0xFF) == 0xEF &&
				(bytes[1] & 0xFF) == 0xBB &&
				(bytes[2] & 0xFF) == 0xBF) {
			return true;
		}
		return false;
	}
*/
	public String fixline(int index, String in) {

		in = in.trim();
		boolean mark = false;
		StringBuilder str = new StringBuilder(in);

		int start = str.indexOf("[");
		int end = str.indexOf("]");
		while (start > -1 && end > -1 && start<end) {
			mark = false;
			if (str.length()-1>end && str.charAt(end+1)==':' )
				end++;
			if (str.length()-1>end && str.charAt(end+1)==' ' ) {					
				mark=true;
				end++;
			}
			if (start>0 && str.charAt(start-1)==' ' && mark==false)
				start--;
			str.replace(start, end+1,"");
			errors.HearingImpairedErrors++;
			start = str.indexOf("[");
			end = str.indexOf("]");				
		}

		start = str.indexOf("(");
		end = str.indexOf(")");				
		while (start > -1 && end > -1 && start<end)	{	
			mark = false;
			if (str.length()-1>end && str.charAt(end+1)==':' )
				end++;
			if (str.length()-1>end && str.charAt(end+1)==' ' ) {
				mark = true;				
				end++;
			}
			if (start>0 && str.charAt(start-1)==' ' && mark==false)
				start--;					
			str.replace(start, end+1,"");
			errors.HearingImpairedErrors++;
			start = str.indexOf("(");
			end = str.indexOf(")");				
		}
		
		
		
		start = str.indexOf("{");
		end = str.indexOf("}");				
		while (start > -1 && end > -1 && start<end)	{	
			mark = false;
			if (str.length()-1>end && str.charAt(end+1)==':' )
				end++;
			if (str.length()-1>end && str.charAt(end+1)==' ' ) {
				mark = true;				
				end++;
			}
			if (start>0 && str.charAt(start-1)==' ' && mark==false)
				start--;					
			str.replace(start, end+1,"");
			errors.HearingImpairedErrors++;
			start = str.indexOf("(");
			end = str.indexOf(")");				
		}


		
		//if (str.indexOf("(")>-1 || str.indexOf(")")>-1 || str.indexOf("[")>-1 || str.indexOf("]")>-1)
		//	badlines.add(index+1);
		
		start = str.indexOf("<i>");
		while (start > -1 ) {
			str.replace(start,start+3,"");
			errors.HearingImpairedErrors++;
			start = str.indexOf("<i>");			
		}
		start = str.indexOf("</i>");
		while (start > -1 ) {
			str.replace(start,start+4,"");
			errors.HearingImpairedErrors++;
			start = str.indexOf("</i>");			
		}		

		start = str.indexOf("<");
		end = str.indexOf(">");				
		while (start > -1 && end > -1 && start<end)	{	
			mark = false;
			if (str.length()-1>end && str.charAt(end+1)==':' )
				end++;
			if (str.length()-1>end && str.charAt(end+1)==' ' ) {
				mark = true;				
				end++;
			}
			if (start>0 && str.charAt(start-1)==' ' && mark==false)
				start--;					
			str.replace(start, end+1,"");
			errors.HearingImpairedErrors++;
			start = str.indexOf("<");
			end = str.indexOf(">");				
		}		
		
		
		if (isNullLine(str))
			str.delete(0,str.length());		
		
		return str.toString();

	}
	
	private boolean isNullLine(StringBuilder str) {
		boolean a=false;
		// TODO Auto-generated method stub
					
		for (int i=0;i<str.length();i++) {
			if (!( str.substring(i).toUpperCase().equals("–") || str.substring(i).toUpperCase().equals("-") || str.substring(i).toUpperCase().equals(" ") ))
				return false;				
		}
		
		return true;
	}

	void removeLongParenthesis(String[] str_in,int index) {
		boolean mark = false;
		String line1 = str_in[0];
		String line2 = str_in[1];
		
		int division = line1.length();
		StringBuilder str = new StringBuilder(line1 + " " + line2);

		int start = str.indexOf("[");
		int end = str.indexOf("]");
		while (start > -1 && end > -1 && start<end) {
			mark = false;
			if (str.length()-1>end && str.charAt(end+1)==':' )
				end++;
			if (str.length()-1>end && str.charAt(end+1)==' ' ) {					
				mark=true;
				end++;
			}
			if (start>0 && str.charAt(start-1)==' ' && mark==false)
				start--;
			str.replace(start, end+1,"");
			if (start<division) {
				division = division - (Math.min(division,end) - start);
			}
			
			errors.HearingImpairedErrors++;
			start = str.indexOf("[");
			end = str.indexOf("]");				
		}

		start = str.indexOf("(");
		end = str.indexOf(")");				
		while (start > -1 && end > -1 && start<end)	{	
			mark = false;
			if (str.length()-1>end && str.charAt(end+1)==':' )
				end++;
			if (str.length()-1>end && str.charAt(end+1)==' ' ) {
				mark = true;				
				end++;
			}
			if (start>0 && str.charAt(start-1)==' ' && mark==false)
				start--;					
			str.replace(start, end+1,"");
			if (start<division) {
				division = division - (Math.min(division,end) - start);
			}
			
			errors.HearingImpairedErrors++;
			start = str.indexOf("(");
			end = str.indexOf(")");
		}

		if (str.indexOf("(")>-1 || str.indexOf(")")>-1 || str.indexOf("[")>-1 || str.indexOf("]")>-1)
			badlines.add(index+1);
		
		str_in[0]=str.substring(0,division);		
		str_in[1]=str.substring(division);

	}


	public boolean Fix() throws IOException {

		if (mStatus) {						

			for (SingleLine line : data) {
				
				//System.out.println(line.index);								
				
				if (line.rows>0)
					line.line1 = fixline(line.real_index,line.line1);						
				if (line.rows>1) {
					line.line2 = fixline(line.real_index,line.line2);
					
					String[] a = {"",""};
					a[0]=line.line1;
					a[1]=line.line2;					
					removeLongParenthesis(a,line.real_index);
					line.line1 = a[0].trim();
					line.line2 = a[1].trim();	
					
				}
				
				line.fix_lines();										
			}

			int tot_lines = data.size();
			SingleLine temp_prev,temp_next,temp;
			//Iterator<SingleLine> iter = data.iterator(),iter_temp;

			ArrayList<Integer> bad_ones = new ArrayList<Integer>();
			ArrayList<Integer> long_ones = new ArrayList<Integer>();

			for (int j = 0; j < tot_lines; j++) {											

				temp = data.get(j);				

				if ( (temp.startTime).largerthan(temp.endTime)) {
					temp.startTime.copy(temp.endTime);					
					temp.update();
					errors.TimingErrors++;
				}

				if (temp.duration > MAX_DURATION) {
					//long_ones.add(j);										
					temp.endTime.copy(temp.startTime);
					temp.endTime.add(MAX_DURATION);
					temp.update();
					errors.TimingErrors++;					
				}


				if (j==0) {
					temp_next = data.get(j+1);	
					if (temp.endTime.largerthan(temp_next.startTime,mTimeOffset)) {
						//bad_ones.add(j);
						//temp.clearLine();
						bad_ones.add(temp.real_index+1);
					}						
				}						
				else if (j==tot_lines-1) {
					temp_prev = data.get(j-1);	
					if (temp_prev.endTime.largerthan(temp.startTime,mTimeOffset)) {
						//bad_ones.add(j);
						//temp.clearLine();
						bad_ones.add(temp.real_index+1);
					}
				}
				else {					
					temp_next = data.get(j+1);
					temp_prev = data.get(j-1);

					if (temp.endTime.largerthan(temp_next.startTime,mTimeOffset) || temp_prev.endTime.largerthan(temp.startTime,mTimeOffset)) {
						//bad_ones.add(j);
						//temp.clearLine();
						bad_ones.add(temp.real_index+1);
					}
				}				
				/*
				if (j==0) {
					//if (temp.duration > MAX_DURATION) {
						temp_next = data.get(j+1);
						if (temp.endTime.largerthan(temp_next.startTime)) {
							temp.endTime.copy(temp_next.startTime);			
							temp.update();
							errors.TimingErrors++;
						}
					}	
				}						
				else if (j==tot_lines-1) {
					if (temp.duration > MAX_DURATION) {
						temp.endTime.copy(temp.startTime);
						temp.endTime.add(MAX_DURATION);
						temp.update();
						errors.TimingErrors++;
					}
				}
				else {					
					temp_next = data.get(j+1);
					temp_prev = data.get(j-1);

					if (temp.endTime.largerthan(temp_next.startTime)) {
						temp.endTime.copy(temp_next.startTime);
						errors.TimingErrors++;
					}

					if (temp_prev.endTime.largerthan(temp.startTime)) {
						temp.startTime.copy(temp_prev.endTime);
						errors.TimingErrors++;
					}

					temp.update();

					if (temp.duration > MAX_DURATION) {
						temp.endTime.copy(temp.startTime);
						temp.endTime.add(MAX_DURATION);
						temp.update();
						errors.TimingErrors++;
					}
				}
				 */


			}

			int ind=0;
			for (int j = 0; j < bad_ones.size(); j++) {
				ind = bad_ones.get(j)-1;
				
				temp = data.get(ind);	
				temp.clearLine();
			}					
							


			
			// PRINT FILE //

			File outFile = new File(destFileNm);			
			
			if (outFile.exists()) {				
				if (!(outFile.delete()))									
					throw new IOException("Failed to create outputfile");
			}
			if (!(outFile.createNewFile()))
					throw new IOException("Failed to create outputfile");			
/*			
			FileWriter ofstream = new FileWriter(outFile);
			BufferedWriter out = new BufferedWriter(ofstream);
*/
			FileOutputStream a=new FileOutputStream(outFile);
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(a,StandardCharsets.ISO_8859_1));

			int i=0;
			for (SingleLine line : data) {
				i++;

				if (line.index !=i)
					errors.IndexErrors++;

				out.write(i + "\n");

				out.write(line.startTime.toString() + " --> " + line.endTime.toString() + "\n");	
				if (line.rows>0)
					out.write(line.line1 + "\n");
				if (line.rows>1)
					out.write(line.line2 + "\n");
				out.write("\n");
				
				if (line.line1_length + line.line2_length > MAX_LINESIZE*4)
					badlines.add(i);

			}

			out.close();

			/* offset algorithm: END */
			alreadyFixed=true;
			
			
			//System.out.println("DONE! Check the result file.");
			return true;
		}
		else {				
			//System.out.println("ERROR: File not read!");
			return false;
		}
	}

	

	/**
	 * Gets the string representation of the number, adding the prefix '0' to
	 * have the required length.
	 * 
	 * @param n
	 *            long number to convert to string.
	 * @param digits
	 *            int number of digits required.
	 * @return String with the required length string (3 for digits = 3 -->
	 *         "003")
	 */
	private static String get2digit(int num, int digits) {
		String result = "" + num;
		while (result.length() < digits) {
			result = "0" + result;
		}
		return result;
	}


	public String strPre(double inValue){
		String shortString = "";

		DecimalFormat df = new DecimalFormat("00.000");
		DecimalFormatSymbols dfs = df.getDecimalFormatSymbols();
		dfs.setDecimalSeparator(',');
		df.setDecimalFormatSymbols(dfs);
		//threeDec.setDecimalSeparator(',');
		shortString = (df.format(inValue));
		return shortString;
	}
	
	public static String testProofRead() {
	String res = "FAILED";
	JLanguageTool langTool = new JLanguageTool(new AmericanEnglish());
	List<RuleMatch> matches;
	try {
		matches = langTool.check("this sentence has errrrror");
		if (!matches.isEmpty())
			res = "";
	} catch (Exception e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
		res = e.getMessage();
	}
	return res;
	}
	
	public void doProofRead(Start myGUI) {
		/*
		JLanguageTool langTool = new JLanguageTool(new BritishEnglish());
		langTool.activateDefaultPatternRules();
		List<RuleMatch> matches;		
		matches = langTool.check("A sentence " +
			    "with a error in the Hitchhiker's Guide tot he Galaxy");
		 
		for (RuleMatch match : matches) {
		  System.out.println("Potential error at line " +
		      match.getLine() + ", column " +
		      match.getColumn() + ": " + match.getMessage());
		  System.out.println("Suggested correction: " +
		      match.getSuggestedReplacements());
		}
		*/
		
		JLanguageTool langTool1 = myGUI.langTool1;
		JLanguageTool langTool2 = myGUI.langTool2;
		
		try {
			List<RuleMatch> matches1,matches2;
			int i=0,k;
			String s,ss;
			List<Pair> lista1 = new ArrayList<Pair>(),lista2=new ArrayList<Pair>(),lista3;						
			for (SingleLine line : data) {
				i++;
				if (line.rows>1)										
					s = line.line1 + " " + line.line2;
				else if (line.rows>0)
					s = line.line1;
				else 
					continue;
				
				matches1 = langTool1.check(s);
				if (!matches1.isEmpty()) {					
					matches2 = langTool2.check(s);	
					if (!matches2.isEmpty()) {

						for (RuleMatch match : matches1)					
							lista1.add(new Pair(match.getColumn()-1, match.getEndColumn()-1));
						for (RuleMatch match : matches2)					
							lista2.add(new Pair(match.getColumn()-1, match.getEndColumn()-1));
						lista3 = JoinLists(lista1,lista2);					
						lista1.clear();
						lista2.clear();

						k=0;

						//char eka,toka,kolmas;
						String heitto="'";
						int heitto_int = (int)heitto.charAt(0);

						for (int j=0;j<lista3.size();j++) {						
							ss = s.substring(lista3.get(j).first,lista3.get(j).second);
							if ( ss.length()>1 && Character.isUpperCase(ss.charAt(0)) && Character.isLowerCase(ss.charAt(1)))
								continue;										
							if ( s.length()>lista3.get(j).second && (int)s.charAt(lista3.get(j).second)==heitto_int )																									
								continue;						
							if (ss=="♪" || (( ss.length()>2) && (ss.charAt(0)=='\u00e2') && (ss.charAt(1)=='\u0099') && (ss.charAt(2)=='\u00aa'))) {

								//eka = ss.charAt(0);
								//toka = ss.charAt(1);
								//kolmas = ss.charAt(2);														
								//String sss = Character.toString(eka)+Character.toString(toka)+Character.toString(kolmas);
								continue;		
							}
							k++;
							if (k==1) ;
							myGUI.printConsole("subtitle " + i + ":\n    ",2); //  match.getMessage()
							//else if (k>1)
							//	myGUI.printConsole(", "); //  match.getMessage()
							myGUI.printConsole("     " + ss +"\n",2);// + match.getSuggestedReplacements()); //  match.getMessage()
							//System.out.print(" [ " +
							//		match.getSuggestedReplacements() + " ]\n");

						}			
						//if (k>0) {
						//	myGUI.printConsole("\n");					
						//}
					}
				}
			}
		}
		catch (Exception ie) {
			ie.printStackTrace();			
			myGUI.printConsole("Proof-read module FAILED:\n"+ie.getMessage(),2);
		}

	}
	
	public List JoinLists(List<Pair> a,List<Pair> b) {
		ArrayList<Pair> lista3 = new ArrayList<Pair>();
		for (int i=0;i<a.size();i++) {
			for (int j=0;j<b.size();j++) {
				if (a.get(i).first==b.get(j).first)
						lista3.add(new Pair(a.get(i).first,a.get(i).second));
			}
		}				
		return lista3;
	}
		
	public String conversion_table(String a) {
	
		String res=a;
	
		if (a.equals("–"))	
			res = "-";
		if (a.equals("’"))	
			res = "'";
		if (a.equals("…"))	
			res = "...";
		if (a.equals("è"))	
				res = "e";
		if (a.equals("ô"))	
				res = "o";
		if (a.equals("î"))	
				res = "i";
		if (a.equals("”"))	
				res = "\"";
		if (a.equals("“"))	
				res = "\"";	
		
		return res;
	
	}
	

}