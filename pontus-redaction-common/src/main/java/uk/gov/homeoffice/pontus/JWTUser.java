package uk.gov.homeoffice.pontus;


import java.security.Principal;

/**
 * Created by leo on 17/10/2016.
 */
public class JWTUser implements HeapSize {

//    protected User user;
    protected String shortName;
    public JWTUser(Principal princ){
        this.shortName = princ.getName();
    }

    public JWTUser(String jwtPath, JWTStore store){

        this.shortName = jwtPath.substring(store.getZkPath().length() + 1);
    }
    public JWTUser(String userString){

        this.shortName = userString;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JWTUser jwtUser = (JWTUser) o;

        return shortName != null ? shortName.equals(jwtUser.shortName) : jwtUser.shortName == null;

    }

    @Override
    public int hashCode() {
        int result = 31  + (shortName != null ? shortName.hashCode() : 0);
        return result;
    }

    @Override
    public long heapSize() {
        return shortName == null? 0 : shortName.length();
    }

    @Override
    public String toString() {
        return "JWTUser{" +
                "shortName='" + shortName + '\'' +
                '}';
    }
}
