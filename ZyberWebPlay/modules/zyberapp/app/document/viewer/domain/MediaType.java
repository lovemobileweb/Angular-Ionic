package document.viewer.domain;

/**
 *
 * @author Alex Bobkov
 */
public enum MediaType {
    APPLICATION_JSON("application/json"), TEXT_HTML("text/html");
    
    private final String name;

    private MediaType(String name) {
        this.name = name;
    }
    
    @Override
    public String toString(){
        return name;
    }
}
