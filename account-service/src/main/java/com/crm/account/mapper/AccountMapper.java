package com.crm.account.mapper;

import com.crm.account.dto.CreateAccountRequest;
import com.crm.account.dto.UpdateAccountRequest;
import com.crm.account.dto.AccountResponse;
import com.crm.account.dto.AccountNoteResponse;
import com.crm.account.dto.AccountTagResponse;
import com.crm.account.dto.AccountAttachmentResponse;
import com.crm.account.dto.AccountActivityResponse;
import com.crm.account.entity.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface AccountMapper {

    Account toEntity(CreateAccountRequest request);

    @Mapping(target = "tags", ignore = true)
    AccountResponse toResponse(Account account);

    void updateEntity(UpdateAccountRequest request, @MappingTarget Account account);

    AccountNoteResponse toNoteResponse(AccountNote note);

    AccountTagResponse toTagResponse(AccountTag tag);

    AccountAttachmentResponse toAttachmentResponse(AccountAttachment attachment);

    AccountActivityResponse toActivityResponse(AccountActivity activity);
}
