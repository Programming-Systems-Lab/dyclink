package edu.columbia.cs.psl.kamino;

public class DirectionalPair {

	private int startFrameID;
	private int endFrameID;

	public DirectionalPair(int startFrameID, int endFrameID) {
		this.startFrameID = startFrameID;
		this.endFrameID = endFrameID;
	}

	public int getStartFrameID() {
		return this.startFrameID;
	}

	public int getEndFrameID() {
		return this.endFrameID;
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
		        ;//&& (this.reached == pair.getReached());
	}

	public String toString() {
		return this.startFrameID + "-" + this.endFrameID ;//+ ((this.reached) ? "X" : "_");
	}
}
