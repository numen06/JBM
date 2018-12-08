package com.jbm.test.devops;

public class RunTest {
	public void print(Object obj) {
		System.out.println(obj);
	}

	public void print(String obj) {
		System.err.println(obj);
	}

	public static void main(String[] args) {
		new RunTest().print("test");
		new RunTest().print(null);
	}
}
