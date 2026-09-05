package com.inventory.staff.dto;

import java.util.List;

public record StaffRosterResponse(
    List<StaffMemberResponse> members,
    List<StaffInviteResponse> pendingInvites
) {
}
