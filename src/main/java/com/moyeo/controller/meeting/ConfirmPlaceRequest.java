package com.moyeo.controller.meeting;

import com.moyeo.service.meeting.ConfirmPlaceCommand;

public record ConfirmPlaceRequest(String commercialAreaCode) {
    public ConfirmPlaceCommand toCommand() { return new ConfirmPlaceCommand(commercialAreaCode); }
}
