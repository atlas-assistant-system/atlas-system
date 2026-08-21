package atlas.domain.home.enums;

public enum NewsCategory {

    TECHNOLOGY("tech", "Tecnología"),
    AI("ai", "IA"),
    DEVELOPMENT("dev", "Desarrollo"),
    DATA("data", "Datos"),
    DEVOPS("devops", "DevOps"),
    SECURITY("infosec", "Seguridad"),
    IT("it", "IT"),
    HARDWARE("hardware", "Hardware");

    private final String slug;
    private final String label;

    NewsCategory(String slug, String label) {
        this.slug = slug;
        this.label = label;
    }

    public String slug() {
        return slug;
    }

    public String label() {
        return label;
    }
}
