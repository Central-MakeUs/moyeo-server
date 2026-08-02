package com.moyeo.service.meeting;

public record GuestEntryResult(String entryType) {

    public static GuestEntryResult newGuest() {
        return new GuestEntryResult("NEW_GUEST");
    }

    public static GuestEntryResult existingGuest() {
        return new GuestEntryResult("EXISTING_GUEST");
    }
}
