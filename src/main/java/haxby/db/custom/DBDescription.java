package haxby.db.custom;

import java.io.Serializable;

public class DBDescription implements Serializable {
	public String name;
	public int type;
	public String path;

	public DBDescription(String name, int type, String path){
		this.name=name;
		this.type=type;
		this.path=path;
	}

	public boolean equals(Object o) {
		if (o instanceof DBDescription) return equals((DBDescription) o);
		return false;
	}

	public boolean equals(DBDescription d){
		return (this.name.equalsIgnoreCase(d.name)&&
				this.path != null && this.path.equalsIgnoreCase(d.path));
	}

	public String toString() {
		if (type==0) return "ASCII File: "+name;
		else if (type==1) return "Excel File: "+name;
		else if (type==2) return "ASCII Url: "+name;
		else if (type==3) return "Excel Url: "+name;
		else return null;
	}
}
