package com.crm.contact.mapper;

import com.crm.contact.dto.*;
import com.crm.contact.entity.Contact;
import com.crm.contact.entity.ContactCommunication;
import com.crm.contact.entity.ContactActivity;
import com.crm.contact.entity.ContactTag;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ContactMapper {

    Contact toEntity(CreateContactRequest request);

    ContactResponse toResponse(Contact contact);

    void updateEntity(UpdateContactRequest request, @MappingTarget Contact contact);

    CommunicationResponse toCommunicationResponse(ContactCommunication entity);

    ContactActivityResponse toActivityResponse(ContactActivity entity);

    ContactTagResponse toTagResponse(ContactTag entity);
}
