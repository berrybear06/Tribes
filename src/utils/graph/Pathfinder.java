package utils.graph;

import utils.Vector2d;

import java.nio.file.Path;
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

    public HashSet<PathNode> nodes;

    public Pathfinder(Vector2d rootPos, NeighbourHelper provider)
    {
        root = new PathNode(rootPos);
        this.provider = provider;
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
        return _findPath(new PathNode(goalPosition));
    }


    private ArrayList<PathNode> _dijkstra()
    {
        root.setTotalCost(0.0);

        ArrayList<PathNode> destinationsFromStart = new ArrayList<>();
        PriorityQueue<PathNode> openList = new PriorityQueue<>();
        HashSet<PathNode> visited = new HashSet<>();

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

    private ArrayList<PathNode> _findPath(PathNode goal)
    {
        ArrayList<PathNode> reachable = _dijkstra();
        for (PathNode n: reachable) {
            if (n.getX() == goal.getX() && n.getY() == goal.getY()) {
                return calculatePath(n);
            }
        }
        return null;
    }
}
