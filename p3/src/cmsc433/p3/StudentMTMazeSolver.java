package cmsc433.p3;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.RecursiveTask;

/**
 * This file needs to hold your solver to be tested. You can alter the class to
 * extend any class that extends MazeSolver. It must have a constructor that
 * takes in a Maze. It must have a solve() method that returns the datatype
 * List<Direction> which will either be a reference to a list of steps to take
 * or will be null if the maze cannot be solved.
 */
public class StudentMTMazeSolver extends SkippingMazeSolver {
	ForkJoinPool pool;
	int proc = Runtime.getRuntime().availableProcessors() + 1;

	public class SolutionNode {
		public SolutionNode parent;
		public Choice choice;

		public SolutionNode(SolutionNode parent, Choice choice) {
			this.parent = parent;
			this.choice = choice;
		}

		public String toString() {
			return choice.at.toString();
		}
	}

	public StudentMTMazeSolver(Maze maze) {
		super(maze);
	}

	/**
	 * Expands a node in the search tree, returning the list of child nodes.
	 * 
	 * @throws SolutionFound
	 */

	/**
	 * Performs a breadth-first search of the maze. The algorithm builds a tree
	 * rooted at the start position. Parent pointers are used to point the way back
	 * to the entrance. The algorithm stores the list of leaves in the variables
	 * "frontier". During each iteration, these leaves are each expanded and the
	 * children the result become the new frontier. If a node represents a dead-end,
	 * it is discarded. Execution stops when the exit is discovered, as indicated by
	 * the SolutionFound exception.
	 */
	public List<Direction> solve() {
		LinkedList<Choice> init = new LinkedList<Choice>();
		LinkedList<Direction> solutionPath = new LinkedList<Direction>();
		LinkedList<DFS> tasks = new LinkedList<DFS>();
		pool = new ForkJoinPool(proc);

		Choice ch = null;

		try {
			init.push(firstChoice(maze.getStart()));

			ch = init.pop();
			int sz = ch.choices.size();
			Direction dd = null;

			for (int i = 0; i < sz; i++) {
				dd = ch.choices.peek();
				tasks.add(new DFS(follow(ch.at, ch.choices.peek()), ch.choices.pop()));
			}

			for (int i = 0; i < sz; i++) {

				List<Direction> res = pool.invoke(tasks.get(i));
				if (res != null) {
					pool.shutdownNow();
					return res;
				}
			}

		} catch (SolutionFound e) {
			solutionPath.push(init.pop().choices.peek());

			return pathToFullPath(solutionPath);
		}
		return null;
	}

	private class DFS extends RecursiveTask<List<Direction>> {
		public Choice node;
		public Direction dir;

		public DFS(Choice n, Direction d) {
			this.node = n;
			this.dir = d;
		}

		private List<Direction> work(Choice node, Direction dir) {

			Choice ch = null;
			LinkedList<Choice> choiceStack = new LinkedList<Choice>();
			Direction add = null;

			try {
				choiceStack.push(node);
				while (!choiceStack.isEmpty()) {
					ch = choiceStack.peek();
					if (ch.isDeadend()) {
						// backtrack.
						choiceStack.pop();
						if (!choiceStack.isEmpty())
							choiceStack.peek().choices.pop();
						continue;
					}
					choiceStack.push(follow(ch.at, ch.choices.peek()));

				}
				// No solution found.

				return null;
			} catch (SolutionFound e) {
				

				Iterator<Choice> iter = choiceStack.iterator();
				LinkedList<Direction> solutionPath = new LinkedList<Direction>();
				solutionPath.push(e.from);
				while (iter.hasNext()) {
					ch = iter.next();

					solutionPath.push(ch.choices.peek());
				}
				solutionPath.push(dir);

				return pathToFullPath(solutionPath);
			}
		}

		@Override
		public List<Direction> compute() {

			LinkedList<DFS> tasks = new LinkedList<DFS>();
			// LinkedList<Direction> res = new LinkedList<Direction>();

			int avail = proc - pool.getPoolSize();
		
			if (avail > 2) {
				
			

				int sz = node.choices.size();
				Direction d = null;

			
				if (sz < avail) {
					avail = sz;
				}
				

				for (int i = 0; i < avail; i++) {
					try {
						d = node.choices.peek();

						tasks.add(new DFS(follow(node.at, d), node.choices.pop()));

					} catch (SolutionFound e) {
						
						List<Direction> exp = new LinkedList<Direction>();

						exp.add(dir);
						exp.add(d);
						return exp;
					}
				}

				for (int i = 0; i < avail; i++) {
					List<Direction> res = pool.invoke(tasks.get(i));
					if (res != null) {
						res.add(dir);
						
						return res;
					}
				}

				for (int i = avail; i < sz; i++) {
					try {
						d = node.choices.peek();
						List<Direction> res = (work(follow(node.at, d), node.choices.pop()));
						if (res != null) {
							res.add(dir);
							return res;
						}

					} catch (SolutionFound e) {
					
						List<Direction> exp = new LinkedList<Direction>();

						exp.add(dir);
						exp.add(d);
						return exp;
					}
				}

			} else {
				return this.work(this.node, this.dir);

			}
			return null;
		}
	}
}
