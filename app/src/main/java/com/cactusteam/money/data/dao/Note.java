package com.cactusteam.money.data.dao;

// THIS CODE IS GENERATED BY greenDAO, EDIT ONLY INSIDE THE "KEEP"-SECTIONS

// KEEP INCLUDES - put your custom includes here
// KEEP INCLUDES END
/**
 * Entity mapped to table "NOTE".
 */
public class Note {

    private Long id;
    private String ref;
    /** Not-null value. */
    private String description;

    // KEEP FIELDS - put your custom fields here
    public static final String TRANSACTION_REF_START = "transaction";
    public static final String TRANSACTION_REF_PATTERN = TRANSACTION_REF_START + "_%d";

    public static final String BUDGET_REF_START = "budget";
    public static final String BUDGET_REF_PATTERN = BUDGET_REF_START + "_%d";

    public static final String DEBT_REF_START = "debt";
    public static final String DEBT_REF_PATTERN = DEBT_REF_START + "_%d";

    public static final String SYNC_ERROR_REF = "sync_error";
    // KEEP FIELDS END

    public Note() {
    }

    public Note(Long id) {
        this.id = id;
    }

    public Note(Long id, String ref, String description) {
        this.id = id;
        this.ref = ref;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    /** Not-null value. */
    public String getDescription() {
        return description;
    }

    /** Not-null value; ensure this value is available before it is saved to the database. */
    public void setDescription(String description) {
        this.description = description;
    }

    // KEEP METHODS - put your custom methods here
    // KEEP METHODS END

}
