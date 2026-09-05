package com.inventory.staff.controller;

import com.inventory.auth.dto.MessageResponse;
import com.inventory.staff.dto.StaffInviteRequest;
import com.inventory.staff.dto.StaffInviteResponse;
import com.inventory.staff.dto.StaffMemberResponse;
import com.inventory.staff.dto.StaffRosterResponse;
import com.inventory.staff.service.StaffService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/staff")
public class StaffController {

    private final StaffService staffService;

    public StaffController(StaffService staffService) {
        this.staffService = staffService;
    }

    @GetMapping
    public StaffRosterResponse roster() {
        return staffService.roster();
    }

    @PostMapping("/invites")
    public StaffInviteResponse invite(@Valid @RequestBody StaffInviteRequest request) {
        return staffService.invite(request);
    }

    @DeleteMapping("/invites/{inviteId}")
    public MessageResponse revoke(@PathVariable UUID inviteId) {
        staffService.revokeInvite(inviteId);
        return new MessageResponse("Invite revoked.");
    }

    @PatchMapping("/members/{membershipId}/deactivate")
    public StaffMemberResponse deactivate(@PathVariable UUID membershipId) {
        return staffService.deactivateMember(membershipId);
    }
}
