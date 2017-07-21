package uk.gov.homeoffice.pontus;


/**
 * Created by leo on 16/10/2016.
 */
public class Namespace implements HeapSize {
    public String namespace;

    public Namespace() {
        this.namespace = null;
    }

    public Namespace(String namespace) {
        this.namespace = namespace;
    }
    public Namespace(StringBuffer namespace) {
        this.namespace = namespace.toString();
    }

    @Override
    public String toString() {
        return namespace.toString() ;
    }

    public Namespace(StringBuilder namespace) {
        this.namespace = namespace.toString();
    }

    @Override
    public long heapSize() {
        return namespace != null ? namespace.length() : 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Namespace namespace1 = (Namespace) o;

        return namespace != null ? namespace.equals(namespace1.namespace) : namespace1.namespace == null;

    }

    @Override
    public int hashCode() {
        return namespace != null ? namespace.hashCode() : 0;
    }
}
