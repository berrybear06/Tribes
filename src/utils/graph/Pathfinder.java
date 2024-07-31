package utils.graph;

import utils.Vector2d;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.PriorityQueue;

/**
 * Created by dperez on 13/01/16.
 */
public class Pathfinder
{
    public PathNode root;
    private NeighbourHelper provider;
    private ArrayList<PathNode> destinationsFromStart;
    private PriorityQueue<PathNode> openList;
    private HashSet<PathNode> visited;

    public Pathfinder(Vector2d rootPos, NeighbourHelper provider)
    {
        root = new PathNode(rootPos, 0.0);
        this.provider = provider;
        destinationsFromStart = new ArrayList<>();
        openList = new PriorityQueue<>();
        visited = new HashSet<>();
    }


    private ArrayList<PathNode> calculatePath(PathNode node)
    {
        ArrayList<PathNode> path = new ArrayList<>();
        while(node != null)
        {
            if(node.getParent() != null) //to avoid adding the start node.
            {
                path.add(0,node);
            }
            node = node.getParent();
        }
        return path;
    }

    //Dijkstraa to all possible destinations. Returns nodes of all destinations.
    public ArrayList<PathNode> findPaths()
    {
        return _dijkstra();
    }

    public ArrayList<PathNode> findPathTo(Vector2d goalPosition)
    {
        return _findPath(goalPosition);
    }


    private ArrayList<PathNode> _dijkstra()
    {
        destinationsFromStart.clear();
        openList.clear();
        visited.clear();

        openList.add(root);
        while (!openList.isEmpty())
        {
            PathNode current = openList.poll();
            if (visited.contains(current)) continue;
            visited.add(current);
            if (current != root) destinationsFromStart.add(current);

            ArrayList<PathNode> neighbours = provider.getNeighbours(current.getPosition(), current.getTotalCost());
            for (PathNode nb : neighbours) {
                if (!visited.contains(nb)) {
                    nb.setTotalCost(current.getTotalCost() + nb.getTotalCost());
                    nb.setParent(current);
                    openList.add(nb);
                }
            }
        }

        return destinationsFromStart;
    }

    private ArrayList<PathNode> _findPath(Vector2d goal)
    {
        ArrayList<PathNode> reachable = _dijkstra();
        for (PathNode n: reachable) {
            if (n.getX() == goal.x && n.getY() == goal.y) {
                return calculatePath(n);
            }
        }
        return null;
    }
}
