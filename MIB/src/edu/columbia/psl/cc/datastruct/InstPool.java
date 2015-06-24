package edu.columbia.psl.cc.datastruct;

import java.util.Comparator;
import java.util.HashMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import edu.columbia.psl.cc.pojo.FieldNode;
import edu.columbia.psl.cc.pojo.InstNode;
import edu.columbia.psl.cc.pojo.MethodNode;
import edu.columbia.psl.cc.util.GlobalRecorder;
import edu.columbia.psl.cc.util.StringUtil;

public class InstPool extends TreeSet<InstNode> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private static Logger logger = Logger.getLogger(InstPool.class);
	
	public static boolean DEBUG;
	
	public static int REGULAR = 0;
	
	public static int METHOD = 1;
	
	public static int FIELD = 2;
	
	private HashMap<String, InstNode> instMap = new HashMap<String, InstNode>();
	
	public InstPool() {
		super(new InstNodeComp());
	}
	
	private void updateTime(InstNode fullInst) {
		long curTime = GlobalRecorder.getCurTime();
		if (fullInst.getStartTime() < 0) {
			/*fullInst.setStartDigit(curTime[1]);
			fullInst.setStartTime(curTime[0]);
			fullInst.setUpdateDigit(curTime[1]);
			fullInst.setUpdateTime(curTime[0]);*/
			fullInst.setStartTime(curTime);
			fullInst.setUpdateTime(curTime);
		} else {
			/*fullInst.setUpdateDigit(curTime[1]);
			fullInst.setUpdateTime(curTime[0]);*/
			fullInst.setUpdateTime(curTime);
		}
 	}
	
	private boolean _addInst(String idxKey, InstNode inst) {
		if (super.add(inst)) {
			this.instMap.put(idxKey, inst);
			return true;
		} else {
			return false;
		}
	}
	
	private boolean _removeInst(String idxKey, InstNode inst) {	
		if (super.remove(inst)) {
			this.instMap.remove(idxKey);
			return true;
		} else {
			return false;
		}
	}
	
	public InstNode searchAndGet(String methodKey, 
			int threadId, 
			int threadMethodIdx, 
			int idx, 
			int opcode, 
			String addInfo, 
			int instType) {
		String idxKey = StringUtil.genIdxKey(threadId, threadMethodIdx, idx);
		if (this.instMap.containsKey(idxKey)) {
			InstNode ret = this.instMap.get(idxKey);
			
			//Handle the case that the instruction type changes (rarely happens, for interface method)
			if ((instType == METHOD) && !(ret instanceof MethodNode)) {
				logger.info("Inst node type changes: " + ret);
				ret.getInstDataParentList();
				ret.getControlParentList();
				
				InstNode newRet = new MethodNode();
				newRet.setFromMethod(ret.getFromMethod());
				newRet.setThreadId(ret.getThreadId());
				newRet.setThreadMethodIdx(ret.getThreadMethodIdx());
				newRet.setIdx(ret.getIdx());
				newRet.setOp(ret.getOp());
				newRet.setAddInfo(ret.getAddInfo());
				newRet.setStartTime(ret.getStartTime());
				newRet.setUpdateTime(ret.getUpdateTime());
				
				//Check this part
				newRet.setRelatedObj(ret.getRelatedObj());
				//newRet.setInstDataParentList(ret.getInstDataParentList());
				//newRet.setControlParentList(ret.getControlParentList());
				//newRet.setChildFreqMap(ret.getChildFreqMap());
				
				this.updateTime(newRet);
				this.remove(ret);
				this._addInst(idxKey, newRet);
				return newRet;
			}
			
			this.updateTime(ret);
			return ret;
		}
		
		InstNode probe = null;
		if (instType == REGULAR) {
			probe = new InstNode();
		} else if(instType == METHOD) {
			probe = new MethodNode();
		} else {
			probe = new FieldNode();
		}
		
		//Create new 
		probe.setFromMethod(methodKey);
		probe.setThreadId(threadId);
		probe.setThreadMethodIdx(threadMethodIdx);
		probe.setIdx(idx);
		probe.setOp(BytecodeCategory.getOpcodeObj(opcode));
		probe.setAddInfo(addInfo);
		this.updateTime(probe);
		this._addInst(idxKey, probe);
		return probe;
	}
	
	public InstNode searchAndGet(String idxKey) {
		if (this.instMap.containsKey(idxKey)) {
			return this.instMap.get(idxKey);
		}
		
		if (DEBUG) {			
			logger.warn("Cannot find inst by method key and idx: " +  idxKey);
		}
		
		return null;
	}
	
	public HashMap<String, InstNode> getInstMap() {
		return this.instMap;
	}
	
	@Override
	public boolean add(InstNode inst) {
		String idxKey = StringUtil.genIdxKey(inst.getThreadId(), 
				inst.getThreadMethodIdx(), 
				inst.getIdx());
		return this._addInst(idxKey, inst);
	}
	
	@Override
	public boolean remove(Object o) {
		if (!(o instanceof InstNode)) {			
			logger.error("Non-compatible object type: " + o.getClass());
			return false;
		}
		
		InstNode inst = (InstNode) o;
		String idxKey = StringUtil.genIdxKey(inst.getThreadId(), 
				inst.getThreadMethodIdx(), 
				inst.getIdx());
		return this._removeInst(idxKey, inst);
	}
	
	/*public InstNode searchAndGet(String methodKey, long threadId, int threadMethodIdx, int idx) {
		for (InstNode inst: this) {
			if (inst.getFromMethod().equals(methodKey) && 
					inst.getThreadId() == threadId && 
					inst.getThreadMethodIdx() == threadMethodIdx && 
					inst.getIdx() == idx)
				return inst;
		}
		
		
		if (DEBUG) {
			logger.warn("Cannot find inst by method key and idx: " +  methodKey + " " + threadId + " " + threadMethodIdx + " " + idx);
		}
		return null;
	}*/
	
	public static class InstNodeComp implements Comparator<InstNode> {

		@Override
		public int compare(InstNode i1, InstNode i2) {
			String i1Idx = StringUtil.genIdxKey(i1.getThreadId(), i1.getThreadMethodIdx(), i1.getIdx());
			String i2Idx = StringUtil.genIdxKey(i2.getThreadId(), i2.getThreadMethodIdx(), i2.getIdx());
			return i1Idx.compareTo(i2Idx);
		}
		
	}
	
	public static void main(String[] args) {
		InstPool pool = new InstPool();
		InstNode i1 = pool.searchAndGet("a", 
				0, 
				0, 
				1, 
				92, 
				"", 
				InstPool.REGULAR);
		InstNode i2 = pool.searchAndGet("a", 
				0, 
				0, 
				2, 
				92, 
				"", 
				InstPool.REGULAR);
		System.out.println("Pool size: " + pool.size());
	}
}
