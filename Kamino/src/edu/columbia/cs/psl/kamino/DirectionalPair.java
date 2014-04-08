package edu.columbia.cs.psl.kamino;

public class DirectionalPair {

	private int writeID;
	private int startFrameID;
	private int endFrameID;

	public DirectionalPair(int startFrameID, int endFrameID) {
		this.startFrameID = startFrameID;
		this.endFrameID = endFrameID;
		this.writeID = -1;
	}
	
	public DirectionalPair(int startFrameID, int endFrameID, int writeID) {
		this.startFrameID = startFrameID;
		this.endFrameID = endFrameID;
		this.writeID = writeID;
	}

	public int getStartFrameID() {
		return this.startFrameID;
	}

	public int getEndFrameID() {
		return this.endFrameID;
	}

	public int getWriteID() {
		return this.writeID;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof DirectionalPair)) {
			return false;
		}
		DirectionalPair pair = (DirectionalPair) obj;
		return (this.startFrameID == pair.getStartFrameID()) && (this.endFrameID == pair.getEndFrameID())
		        && (this.writeID == pair.getWriteID());
	}

	public String toString() {
		String written = (this.writeID == this.endFrameID) ? "write " : "read (written in " + this.writeID + ") ";
		return written + "from " + this.startFrameID + " to " + this.endFrameID;
	}
}
