import ch.lowres.jlo.Server;
import ch.lowres.jlo.Message;
import ch.lowres.jlo.NetAddress;

import com.sun.jna.Callback;
import com.sun.jna.Pointer;

//tb/130702//130704//131112

//javac -cp .:lib/jlo.jar:lib/jna-3.3.0.jar OClock.java
//LD_LIBRARY_PATH=/usr/local/lib java -cp .:lib/jna-3.3.0.jar:lib/jlo.jar OClock 1234

public class OClock
{
	//osc default port
	static String listeningPort="9999";

	static Component c1;
	static Component c2;
	static Component c3;
	static Component c4;

	static Component head=null;
	static Component tail=null;
	static Component selected=null;
	static Component lastSelected=selected;

	//output display setting:
	//true: print clock string in place (update previous clock)
	//false: print every updated clock string on a new line
	static boolean displayInPlace=false;

	//output display setting:
	//true: verbose / debug output
	//false: no debug output
	static boolean printAction=false;

	//data entry setting:
	//true: jump to next component on finished entry
	//false: keep current component selected for entry
	static boolean autoSelectNext=true;

	//data entry setting:
	//true: if min or max value of component reached, do not change left-side (higher value) components
	//false: if min or max value of component reached, change left-side (higher value) components
	static boolean standaloneComponents=false;

	static boolean entryOngoing=false;
	static boolean additionOngoing=false;
	static boolean subtractionOngoing=false;
	static int entryCount=0;
	static int currentValue=0;
	static boolean clockActive=true;

	//counter for osc requests
	static long receiveCount=0l;

	static String selectedDelimStart="[";
	static String selectedDelimEnd="]";

	static String entryDelimStart="(";
	static String entryDelimEnd=")";

//=============================================

	public OClock()
	{
		setupHMSmsClock();
		printClock("init");
		setupServer();
	}

//=============================================

	public static void main(String[] args)
	{
		if(args.length>0 && ( args[0].equals("-h") || args[0].equals("--help") ))
		{
			println("\nOClock HELP\n");
			println("Optional parameter: <portnumber>\n");
			println("OSC server default port: "+listeningPort+"\n\n");
			System.exit(0);
		}
		else if(args.length>0)
		{
			listeningPort=args[0];
		}
		OClock c=new OClock();
	}

//=============================================

	static public void setupHMSmsClock()
	{
		/*
                     02:25:16.333
                _______________________________________
                |                                     |
                |-->[HH]<--->[MM]<--->[SS]<--->[MS]<--|
                     c1       c2       c3       c4
                    head                       tail

		*/
		c1=new Component("hours",0,23,0,2,null,null,":");
		c2=new Component("minutes",0,59,0,2,c1,null,":");
		c3=new Component("seconds",0,59,0,2,c2,null,".");
		c4=new Component("milliseconds",0,999,0,3,c3,null,"");
		head=c1;
		tail=c4;
		head.right=c2;
		c2.right=c3;
		c3.right=c4;
		//circular
		tail.right=head;
		head.left=tail;
		selected=head;
	}

//=============================================

	public static void resetClock()
	{
		head.resetAll();
	}

	public static String getClockString()
	{
		return head.getValueStringFull("");
	}

	public static String getClockStringDigitsOnly()
	{
		return head.getClockStringDigitsOnly("");
	}

	public static void printClockAction(String sAction)
	{
		println(sAction+"\t\t"+getClockString());
	}

	public static void printClock(String sAction)
	{
		if(printAction)
		{
			printClockAction(sAction);
		}
		else if(displayInPlace)
		{
			System.out.print("\r"+getClockString());
		}
		else
		{
			println(getClockString());
			//println(getClockStringDigitsOnly());
		}
	}

	static void checkOngoing()
	{
		if(additionOngoing)
		{
			//a+b = b+a
			additionOngoing=false;
			selected.add(currentValue);
		}
		else if(subtractionOngoing)
		{
			//a-b != b-a
			subtractionOngoing=false;
			int sub=selected.value;
			selected.value=currentValue;
			selected.sub(sub);
		}
		entryOngoing=false;
	}

//=============================================
//=============================================

	/*inner class representing one token/part of a clock*/
	static class Component
	{
		String name="";
		int min=0;
		int max=0;
		int value=0;
		int digitCount=0;
		Component left=null;
		Component right=null;
		//right side
		String seperator="";

		public Component(String name, int min, int max, int value, int digitCount, Component left, Component right, String seperator)
		{
			this.name=name;
			this.min=min;
			this.max=max;
			this.value=value;
			this.digitCount=digitCount;
			this.left=left;
			this.right=right;
			this.seperator=seperator;
		}

		public String withLeadingZeros(int val,int count)
		{
			if(count<=0)
			{
				return ""+val;
			}

			return String.format("%0"+count+"d", val);
		}

		public String withLeadingSpaces(int val,int count)
		{
			if(count<=0)
			{
				return ""+val;
			}

			return String.format("%"+count+"s", val);
		}

		public String getValueString()
		{
			return withLeadingZeros(value,digitCount);
		}

		public String getEntryString()
		{
			return withLeadingSpaces(value,digitCount);
		}

		public String getValueStringFull(String s)
		{
			String pre="";
			String post="";
			String sep=seperator;
			if(this.equals(selected))
			{
				if(entryOngoing)
				{
					pre=entryDelimStart;
					post=entryDelimEnd;
				}
				else
				{
					pre=selectedDelimStart;
					post=selectedDelimEnd;
				}
			}
			if(!this.equals(tail))
			{
				if(additionOngoing && right.equals(selected))
				{
					sep="+";
				}
				else if(subtractionOngoing && right.equals(selected))
				{
					sep="-";
				}
				return right.getValueStringFull(s+pre+withLeadingZeros(value,digitCount)+post+sep);
			}
			else
			{
				return s+pre+withLeadingZeros(value,digitCount)+post;
			}
		}

		public String getClockStringDigitsOnly(String s)
		{
			if(!this.equals(tail))
			{
				return(right.getClockStringDigitsOnly(s+getValueString()));
			}
			else
			{
				return s+getValueString();
			}
		}

		public void reset()
		{
			value=min;
		}

		public void resetAll()
		{
			reset();
			if(!this.equals(tail))
			{
				right.resetAllAfter(1);
			}
			if(!this.equals(head))
			{
				left.resetAllBefore(1);
			}
		}

		public void resetAllAfter(int i)
		{
			if(i>0)
			{
				reset();
			}
			i++;
			if(!this.equals(tail))
			{
				right.resetAllAfter(i);
			}
		}

		public void resetAllBefore(int i)
		{
			if(i>0)
			{
				reset();
			}
			i++;
			if(!this.equals(head))
			{
				left.resetAllBefore(i);
			}
		}

		public void increment()
		{
			add(1);
		}

		public void decrement()
		{
			sub(1);
		}

		public boolean allLeftAreMin()
		{
			if(this.equals(head))
			{
				if(value == min || this.equals(selected) )
				{
					return true;
				}
				else
				{
					return false;
				}
			}
			else if(value>min && !this.equals(selected))
			{
				return false;
			}
			else
			{
				return left.allLeftAreMin();
			}
		}

		public boolean allLeftAreMax()
		{
			if(this.equals(head))
			{
				if(value == max || this.equals(selected))
				{
					return true;
				}
				else
				{
					return false;
				}
			}
			else if(value<max && !this.equals(selected))
			{
				return false;
			}
			else
			{
				return left.allLeftAreMax();
			}
		}

		public void sub(int sub)
		{
			if(value-sub<min)
			{
				int remain=( (max+1) + (value-sub) % (max+1) ) % (max+1);
				int leftSub=(int) Math.floor((value-sub) / (max+1)) + 1;

				if(standaloneComponents)
				{
					value=remain;
				}
				else if(allLeftAreMin())
				{
					value=min;
				}
				else if(!standaloneComponents && !this.equals(head) && !allLeftAreMin())
				{
					value=remain;
					left.sub(leftSub);
				}
			}
			else
			{
				value-=sub;
			}
		}

		public void add(int add)
		{
			if(value+add>max)
			{
				int remain=(value+add) % (max+1);
				int leftAdd=(int) Math.floor((value+add) / (max+1));

				if(standaloneComponents)
				{
					value=remain;
				}
				else if(allLeftAreMax())
				{
					value=max;
				}
				else if(!standaloneComponents && !this.equals(head) && !allLeftAreMax())
				{
					value=remain;
					left.add(leftAdd);
				}
			}
			else
			{
				value+=add;
			}
		}

		public int leftValueSum(int i)
		{
			if(!this.equals(head))
			{
				return left.leftValueSum(i+value);
			}
			else
			{
				return i+value;
			}
		}

		public void describe()
		{
			println(name+" "+min+" "+max+" "+withLeadingZeros(value,digitCount));
		}

	}//end Component

//=============================================
//=============================================

	/*process input / operation event*/

	static boolean processAction(String s)
	{
		//toggle clock active
		if(s.equals("^"))
		{
			checkOngoing();
			if(clockActive)
			{
				clockActive=false;
				lastSelected=selected;
				selected=null;
			}
			else
			{
				clockActive=true;
				selected=lastSelected;
			}
		}
		//set clock inactive
		else if(s.equals("q") || s.equals("escape"))
		{
			checkOngoing();
			clockActive=false;
			selected=null;
		}
		//set clock active
		else if(s.equals("a"))
		{
			checkOngoing();
			clockActive=true;
			selected=head;
		}

		//if clock inactive, stop here
		else if(!clockActive)
		{
			return false;
		}
		else if(s.equals("z"))
		{
			autoSelectNext=true;
		}
		else if(s.equals("u"))
		{
			autoSelectNext=false;
		}
		//toggle
		else if(s.equals("i") || s.equals("insert"))
		{
			autoSelectNext=!autoSelectNext;
		}
		else if(s.equals("j"))
		{
			standaloneComponents=true;
		}
		else if(s.equals("k"))
		{
			standaloneComponents=false;
		}
		//toggle
		else if(s.equals("l"))
		{
			standaloneComponents=!standaloneComponents;
		}
		//navigate to left component
		else if(s.equals("d") || s.equals("arrow_left"))
		{
			checkOngoing();
			selected=selected.left;
		}
		//navigate to right component
		else if(s.equals("g") || s.equals("arrow_right"))
		{
			checkOngoing();
			selected=selected.right;
		}
		//increment component value by 1
		else if(s.equals("r") || s.equals("arrow_up"))
		{
			entryOngoing=false;
			additionOngoing=false;
			subtractionOngoing=false;
			selected.increment();
		}
		//decrement component value by 1
		else if(s.equals("v") || s.equals("arrow_down"))
		{
			entryOngoing=false;
			additionOngoing=false;
			subtractionOngoing=false;
			selected.decrement();
		}
		//increment component value by 10^(digitcount -1)
		else if(s.equals("t") || s.equals("page_up"))
		{
			entryOngoing=false;
			additionOngoing=false;
			subtractionOngoing=false;
			int stepSize=(int)Math.pow(10,selected.digitCount-1);
			selected.add(stepSize);
		}
		//decrement component value by 10^(digitcount -1)
		else if(s.equals("b") || s.equals("page_down"))
		{
			entryOngoing=false;
			additionOngoing=false;
			subtractionOngoing=false;
			int stepSize=(int)Math.pow(10,selected.digitCount-1);
			selected.sub(stepSize);
		}
		//select head
		else if(s.equals("h"))
		{
			checkOngoing();
			selected=head;
		}
		//select 2nd comp
		else if(s.equals("m"))
		{
			checkOngoing();
			selected=head.right;
		}
		//select 3rd comp
		else if(s.equals("s"))
		{
			checkOngoing();
			selected=head.right.right;
		}
		//select tail
		else if(s.equals("x"))
		{
			checkOngoing();
			selected=tail;
		}
		//clear (zero) all components after currently selected
		else if(s.equals(","))
		{
			checkOngoing();
			selected.resetAllAfter(0);
		}
		//clear all components before currently selected
		else if(s.equals(";"))
		{
			checkOngoing();
			selected.resetAllBefore(0);
		}
		//clear all components before and after currently selected
		else if(s.equals(":"))
		{
			checkOngoing();
			selected.resetAllBefore(0);
			selected.resetAllAfter(0);
		}
		//clear currently selected component
		else if(s.equals("o") || s.equals("backspace"))
		{
			entryOngoing=false;
			additionOngoing=false;
			subtractionOngoing=false;
			selected.value=selected.min;
			if(!standaloneComponents && !selected.equals(tail))
			{
				selected=selected.right;
			}
		}
		//clear all components
		else if(s.equals("p") || s.equals("delete"))
		{
			entryOngoing=false;
			additionOngoing=false;
			subtractionOngoing=false;
			resetClock();
		}
		//finish entry and/or jump to next component
		else if(s.equals(".") || s.equals("") || s.equals("enter"))
		{
			boolean bOn=false;
			if(additionOngoing || subtractionOngoing || entryOngoing)
			{
				bOn=true;
			}
			checkOngoing();
			if(!selected.equals(tail) && (!bOn || autoSelectNext))
			{
				selected=selected.right;
			}
		}
		//start addition to currently selected component entry
		else if(s.equals("+"))
		{
			entryOngoing=true;
			additionOngoing=true;
			subtractionOngoing=false;
			currentValue=selected.value;
			entryCount=0;
			selected.value=0;
		}
		//start subtraction to currently selected component entry
		else if(s.equals("-"))
		{
			entryOngoing=true;
			additionOngoing=false;
			subtractionOngoing=true;
			currentValue=selected.value;
			entryCount=0;
			selected.value=0;
		}
		//any digit
		else if(s.equals("0") 
			|| s.equals("1") 
			|| s.equals("2") 
			|| s.equals("3") 
			|| s.equals("4") 
			|| s.equals("5") 
			|| s.equals("6") 
			|| s.equals("7") 
			|| s.equals("8") 
			|| s.equals("9"))
		{
			int val=0;
			try
			{
				if(!entryOngoing)
				{
					entryOngoing=true;
					entryCount=0;
					selected.value=0;
				}
				val=Integer.parseInt(s);
				entryCount++;
				int newValue=val+selected.value*10;
				selected.value=newValue;
				//println("debug: "+selected.getEntryString());
				if(entryCount>=selected.digitCount)
				{
					entryCount=0;
					entryOngoing=false;
					if(additionOngoing)
					{
						//a+b = b+a
						additionOngoing=false;
						//selected.value=selected.value+currentValue;
						selected.add(currentValue);
					}
					else if(subtractionOngoing)
					{
						//a-b != b-a
						subtractionOngoing=false;
						//selected.value=selected.value+currentValue;
						int sub=selected.value;
						selected.value=currentValue;
						selected.sub(sub);
					}
					else
					{
						//limit direct entry to max of component
						if(newValue>selected.max)
						{
							selected.value=selected.max;
						}
					}

					if(!selected.equals(tail) && autoSelectNext)
					{
						selected=selected.right;
					}
				}//end if
			}//end try
			catch(Exception e)
			{
				return false;
			}
		}//end if/else if
		else //unknown action
		{
			return false;
		}
		//default return true
		return true;
	}//end processAction

	static boolean setClock(String s)
	{
		boolean ret=false;
		for (int index=0;index<s.length();index++)
		{
     			char c = s.charAt(index);
			//println(c);
			ret=processAction(""+c);
			if(!ret)
			{
				break;
			}
		}
		return ret;
	}

//=============================================

	/*osc related*/

	public static void setupServer()
	{
		Server srv=null;
		try
		{
			srv=new Server(listeningPort);
		}
		catch(Exception e)
		{
			println(e.getMessage());
			System.exit(1);
		}

		final Server srv_=srv;

		ECALL ec=new ECALL();

		//send single instructions (keystrokes, sendkey):
		//oscsend localhost 9999 /key s '^'
		srv_.addMethod("/key","s",ec);

		//compatibility to sk/sendkeys https://github.com/7890/sendkeys
		srv_.addMethod("/key","is",ec);

		/*
		send a series of ops
		example: activate clock for entry, reset all components, 
		navigate to minutes component, set to 4, 
		navigate to milli seconds component, sub 2, 
		navigate to hours component. ready for entry: (00):03:59.998
		oscsend localhost 9999 /set s 'apm4x-2h'
		*/
		srv_.addMethod("/set","s",ec);

		//format output for shout coloring:
		//oscsend localhost 9999 /delim ssss '\\[' '\\]' '\\(' '\\)'
		srv_.addMethod("/delim","ssss",ec);

		new Thread()
		{
			public void run()
			{
				//wait for incoming messages
				while(true)
				{
					try
					{
						Thread.sleep(1);
						srv_.recv();
					}
					catch(Exception e)
					{
						println("ERROR "+e.getMessage());
					}
        			}
			}
		}.start();
	}//end setupServer

	public interface EventCallback extends Callback 
	{
		boolean newOscMatch(String path, String types, Pointer lo_args, int argc, Pointer lo_message, Pointer user_data);
	}

	static class ECALL implements EventCallback 
	{
		public boolean newOscMatch(String path, String types, Pointer lo_args, int argc, Pointer lo_message, Pointer user_data)
		{
			receiveCount++;

			Message msg=new Message(lo_message);
			String s="";

			String clockSave=getClockStringDigitsOnly();
			Component selectionSave=selected;
			boolean ret=true;

			if(path.equals("/key"))
			{
				// /key s
				if(argc==1)
				{
					s=msg.getString(0);
				}
				// /key is
				else if (argc==2)
				{
					s=msg.getString(1);
				}
				ret=processAction(s);

			}
			else if(path.equals("/set"))
			{
				ret=setClock(s);
			}
			else if(path.equals("/delim"))
			{
				selectedDelimStart=msg.getString(0);
				selectedDelimEnd=msg.getString(1);
				entryDelimStart=msg.getString(2);
				entryDelimEnd=msg.getString(3);
				ret=true;
			}
			
			if(!ret)
			{
				//println("restoring clock");
				setClock("h"+clockSave);
				selected=selectionSave;
			}

			printClock(s);

/*
			//debug
			NetAddress na=msg.getSource();

			println("host "+na.getHostname());
			println("port "+na.getPort());
			println("proto "+na.getProtocol());
			println("url "+na.getUrl());
			println("path "+path);
*/
			//take care
			//System.gc();

			return ret;

		}//end newOscMatch
	}//end class ECALL

//=============================================

	public static void println(Object o)
	{
		System.out.println(""+o);
	}


}//end class Clock
