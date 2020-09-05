package jkind.realizability.engines;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

/* Partitioning of specification for delta debugging. The class splits the specification in a similar way to how
   numpy.array_split works in python. Given a list L of size l, we split it in n parts and receive l%n parts of size
   l/n + 1, while the rest are of size l/n.
* */

//public final class Partition<T> extends AbstractList<List<T>> {
public final class Partition<T> {
    private final List<T> list;
    private final int parts;

    public Partition(List<T> list, int parts) {
        this.list = list;
        this.parts = parts;
    }

    public static <T> Partition<T> inParts(List<T> list, int parts) {
        return new Partition<>(list, parts);
    }

//    @Override
//    public List<T> get(int index) {
//        int start, end;
//        int size = list.size();
//        if (index < size % parts) {
//            start = index * (size % parts - 1);
//            end = start + (size / parts + 1);
//        } else {
//            if (size % parts != 0) {
//                start = (size % parts) * (size / parts + 1) + index % (size % parts);
//            } else {
//                start = index*(size / parts);
//            }
//            end = start + (size / parts);
//        }
//
//        if (start > end) {
//            throw new IndexOutOfBoundsException("Index " + index + " is out of the list range <0," + (size() - 1) + ">");
//        }
////        System.out.println("The indexed partition  is : [" + start + "," + end + "] " + " and the list is : " + list + " and the parts are : " + parts + "and the index is : " + index);
//        ArrayList partition = new ArrayList<>();
//        partition.addAll(list.subList(start,end));
//        return partition;
//    }

    public List<T> get(int index) {
        int size = list.size();
        ArrayList partition = new ArrayList();
        if (index < size % parts) {
            for (int i = 0; i <= size / parts; i++) {
                partition.add(list.get(index*(size / parts + 1) + i));
            }
        } else {
            for (int i = 0; i <= size / parts - 1; i++) {
                int actualIndex = (size % parts)*(size / parts + 1) + (index - size % parts)*(size / parts);
                partition.add(list.get(actualIndex + i));
            }
        }
        return partition;
    }

//    @Override
    public int size() {
        return parts;
    }

    public List<T> getComplement(int index) {
        List<T> complement = new ArrayList<>(list);
        complement.removeAll(get(index));
        return complement;
    }
}
