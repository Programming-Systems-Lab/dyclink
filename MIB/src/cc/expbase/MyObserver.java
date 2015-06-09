package cc.expbase;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Stack;

public class MyObserver extends Observable implements Observer {
	
	public List<String> myDeps;
	
	public Stack<MyObserver> children = new Stack<MyObserver>();
	
	public String name;
	
	public MyObserver(List<String> deps) {
		this.myDeps = deps;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getName() {
		return this.name;
	}
	
	public void addDeps(List<String> newDeps) {
		this.myDeps.addAll(newDeps);
	}

	@Override
	public void update(Observable o, Object arg) {
		MyObserver theObservable = (MyObserver)o;
		
		System.out.println("Received observable: " + theObservable.getName());
		System.out.println("The deps: " + theObservable.myDeps);
		
		this.children.peek().addDeps(theObservable.myDeps);
	}
	
	public void migrateDependency() {
		this.setChanged();
		this.notifyObservers();
	}
	
	public void addChildren(MyObserver child) {
		this.children.push(child);
		child.addObserver(this);
	}
	
	public void removeChild() {
		MyObserver removed = this.children.pop();
		removed.migrateDependency();
	}
	
	public static void main(String[] args) {
		List<String> parentList = new ArrayList<String>();
		parentList.add("1");
		MyObserver parent = new MyObserver(parentList);
		parent.setName("parent");
		
		List<String> c1List = new ArrayList<String>();
		c1List.add("2");
		MyObserver child1 = new MyObserver(c1List);
		child1.setName("child1");
		
		List<String> c2List = new ArrayList<String>();
		c2List.add("3");
		MyObserver child2 = new MyObserver(c2List);
		child2.setName("child2");
		
		parent.addChildren(child1);
		parent.addChildren(child2);
		
		System.out.println("C1 observer count: " + child1.countObservers());
		System.out.println("C2 observer count: " + child2.countObservers());
		
		parent.removeChild();
		System.out.println("Check children after remove");
		System.out.println(parent.children.peek().getName());
		System.out.println(parent.children.peek().myDeps);
	}

}
