package org.axonframework.sample.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.axonframework.commandhandling.CommandBus;
import org.axonframework.commandhandling.CommandCallback;
import org.axonframework.commandhandling.GenericCommandMessage;
import org.axonframework.eventhandling.EventBus;
import org.axonframework.eventhandling.annotation.AnnotationEventListenerAdapter;
import org.axonframework.eventhandling.annotation.EventHandler;
import org.axonframework.sample.app.api.AddressAddedEvent;
import org.axonframework.sample.app.api.AddressChangedEvent;
import org.axonframework.sample.app.api.AddressRemovedEvent;
import org.axonframework.sample.app.api.AddressType;
import org.axonframework.sample.app.api.ChangeContactNameCommand;
import org.axonframework.sample.app.api.ContactCreatedEvent;
import org.axonframework.sample.app.api.ContactDeletedEvent;
import org.axonframework.sample.app.api.ContactNameAlreadyTakenException;
import org.axonframework.sample.app.api.ContactNameChangedEvent;
import org.axonframework.sample.app.api.CreateContactCommand;
import org.axonframework.sample.app.api.RegisterAddressCommand;
import org.axonframework.sample.app.api.RemoveAddressCommand;
import org.axonframework.sample.app.api.RemoveContactCommand;
import org.axonframework.sample.app.query.AddressEntry;
import org.axonframework.sample.app.query.ContactEntry;
import org.axonframework.sample.app.query.ContactRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

@ContextConfiguration(locations = "classpath:cucumber.xml")
public class ContactFeatureStepDefinitions {

	@Autowired
	private CommandBus commandBus;

	@Autowired
	private EventBus eventBus;

	@Autowired
	private ContactRepository queryContactRepository;

	static Logger logger = LoggerFactory
			.getLogger(ContactFeatureStepDefinitions.class);

	private ContactEntry contact;

	private AnnotationEventListenerAdapter eventListener;

	@Before
	public void init() {
		eventListener = new AnnotationEventListenerAdapter(this, eventBus);
	}

	@After
	public void teardown() {
		eventListener.unsubscribe();
		eventListener = null;
	}

	public void dispatch(Object command) throws Throwable {
		commandBus.dispatch(new GenericCommandMessage<Object>(command),
				new CommandCallback<Object>() {

					@Override
					public void onFailure(Throwable throwable) {
						throw new RuntimeException(throwable);
					}

					@Override
					public void onSuccess(Object result) {
						// great! The event was posted!
					}
				});
	}

	private void loadContact(String contactId) {
		contact = queryContactRepository.loadContactDetails(contactId);
	}

	private ContactCreatedEvent contactCreatedEvent;

	@Before
	public void clearContactCreatedEvent() {
		contact = null;
		contactCreatedEvent = null;
	}

	@EventHandler
	public void handle(ContactCreatedEvent event) {
		contactCreatedEvent = event;
		loadContact(event.getContactId());
	}

	@Given("^an empty address book$")
	public void an_empty_address_book() throws Throwable {
		List<ContactEntry> contacts = queryContactRepository.findAllContacts();
		eventListener.unsubscribe();
		for (ContactEntry contact : contacts) {
			RemoveContactCommand command = new RemoveContactCommand();
			command.setContactId(contact.getIdentifier());
			dispatch(command);
		}
		eventListener.subscribe();
		contacts = queryContactRepository.findAllContacts();
		assertEquals("The address book is not empty", 0, contacts.size());
	}

	@When("^I add a contact with the name \"(.*?)\"$")
	public void i_add_a_contact_with_the_name(String contactName)
			throws Throwable {
		CreateContactCommand command = new CreateContactCommand();
		command.setNewContactName(contactName);
		dispatch(command);
	}

	@Then("^the contact \"(.*?)\" is added to the address book$")
	public void the_contact_is_added_to_the_address_book(String contactName)
			throws Throwable {
		assertTrue(findContactInAddressBook(contactName));
		assertNotNull(contactCreatedEvent);
	}

	private boolean findContactInAddressBook(String contactName) {
		contact = null;
		List<ContactEntry> contacts = queryContactRepository.findAllContacts();
		for (ContactEntry contact : contacts) {
			if (contactName.equals(contact.getName())) {
				this.contact = contact;
				break;
			}
		}
		return contact != null;
	}

	@Given("^the contact \"(.*?)\"$")
	public void the_contact(String contactName) throws Throwable {
		if (!findContactInAddressBook(contactName)) {
			i_add_a_contact_with_the_name(contactName);
		}
	}

	@When("^I change the name to \"(.*?)\"$")
	public void i_change_the_name_to(String newName) throws Throwable {
		ChangeContactNameCommand command = new ChangeContactNameCommand();
		command.setContactId(contact.getIdentifier());
		command.setContactNewName(newName);
		dispatch(command);
	}

	private ContactNameChangedEvent contactNameChangedEvent;

	@Before
	public void clearContactNameChangedEvent() {
		contactNameChangedEvent = null;
	}

	@EventHandler
	public void handle(ContactNameChangedEvent event) {
		contactNameChangedEvent = event;
		loadContact(event.getContactId());
	}

	@Then("^the name is changed to \"(.*?)\"$")
	public void the_name_is_changed_from_to(String newName) throws Throwable {
		assertNotNull(contactNameChangedEvent);
		loadContact(contactNameChangedEvent.getContactId());
		assertNotNull(contact);
		assertEquals(newName, contact.getName());
	}

	@When("^I delete this contact$")
	public void i_delete_this_contact() throws Throwable {

		RemoveContactCommand command = new RemoveContactCommand();
		command.setContactId(contact.getIdentifier());
		dispatch(command);

	}

	private ContactDeletedEvent contactDeletedEvent;

	@Before
	public void clearContactDeletedEvent() {
		contactDeletedEvent = null;
	}

	@EventHandler
	public void handle(ContactDeletedEvent event) {
		contactDeletedEvent = event;
		contact = null;
	}

	@Then("^the contact \"(.*?)\" can no longer be found in the address book$")
	public void the_contact_can_no_longer_be_found_in_the_address_book(
			String contactName) throws Throwable {
		assertFalse(findContactInAddressBook(contactName));
		assertNotNull(contactDeletedEvent);
		assertNull(contact);
	}

	Throwable duplicateContactException;

	@When("^I add a contact with the same name$")
	public void i_add_a_contact_with_the_same_name() throws Throwable {
		CreateContactCommand command = new CreateContactCommand();
		command.setNewContactName(contact.getName());
		commandBus.dispatch(new GenericCommandMessage<Object>(command),
				new CommandCallback<Object>() {

					@Override
					public void onFailure(Throwable throwable) {
						throwable.printStackTrace(System.err);
						duplicateContactException = throwable;
					}

					@Override
					public void onSuccess(Object result) {
						assertTrue("duplicate contact succeeded!", false);
					}
				});
	}

	@Then("^I receive a message that this contact already exists$")
	public void i_receive_a_message_that_this_contact_already_exists()
			throws Throwable {
		assertNotNull(duplicateContactException);
		System.err.println(duplicateContactException);
		assertEquals(ContactNameAlreadyTakenException.class.getCanonicalName(),
			duplicateContactException.getClass().getCanonicalName());
	}

	@Before
	public void clearAddressAddedEvents() {
		addressAddedEvents = null;
		addressAddedEvents = new ArrayList<AddressAddedEvent>();
	}

	List<AddressAddedEvent> addressAddedEvents;

	@EventHandler
	public void handle(AddressAddedEvent event) {
		addressAddedEvents.add(event);
	}

	@When("^I add the following address(?:es)?:$")
	public void i_add_the_following_addresses(
			List<RegisterAddressCommand> commands) throws Throwable {
		for (RegisterAddressCommand command : commands) {
			command.setContactId(contact.getIdentifier());
			System.out.println("adding " + command.getStreetAndNumber() + "to"  + " " + contact.getIdentifier());
			dispatch(command);
		}
	}

	@Then("^i can find the contact if i search for all contacts in \"(.*?)\"$")
	public void i_can_find_the_contact_if_i_search_for_all_contacts_in(
			String city) throws Throwable {
		boolean found = false;
		List<AddressEntry> addresses = queryContactRepository
				.findAllAddressesInCityForContact(null, city);
		for (AddressEntry address : addresses) {
			found = found
					|| contact.getIdentifier().equals(address.getIdentifier());
		}
		assertTrue("contact not found", found);
	}

	private AddressRemovedEvent addressRemovedEvent;

	@Before
	public void clearAddressRemovedEvent() {
		addressRemovedEvent = null;
	}

	@EventHandler
	public void handle(AddressRemovedEvent event) {
		System.out.println("removed " + event.getType());
		addressRemovedEvent = event;
	}

	@When("^delete the \"(.*?)\" address$")
	public void delete_the_address(AddressType addressType) throws Throwable {
		RemoveAddressCommand command = new RemoveAddressCommand();
		System.out.println("delete " + addressType + " address from contactId:"
				+ contact.getIdentifier());
		command.setContactId(contact.getIdentifier());
		command.setAddressType(addressType);
		dispatch(command);
		assertTrue(addressRemovedEvent != null);
	}

	@Then("^the contact has a \"(.*?)\" and \"(.*?)\" address left$")
	public void the_contact_has_a_and_address_left(AddressType addressType1,
			AddressType addressType2) throws Throwable {
		List<AddressEntry> addresses = queryContactRepository
				.findAllAddressesForContact(contact.getIdentifier());
		Set<AddressType> types = new HashSet<AddressType>();
		for (AddressEntry address : addresses) {
			types.add(address.getAddressType());
		}
		System.out.println("contactId:" + contact.getIdentifier());
		types.remove(addressType1);
		types.remove(addressType2);
		assertEquals(
				"not exactly two addresses found of contact "
						+ contact.getName(), 2, addresses.size());
		assertTrue("addresses of type " + types.toString() + " found.",
				0 == types.size());
	}

	private AddressChangedEvent addressChangedEvent;

	@Before
	public void clearAddressChangedEvent() {
		addressChangedEvent = null;
	}

	@EventHandler
	public void handle(AddressChangedEvent event) {
		addressChangedEvent = event;
	}

	@Then("^the contact's private address has moved to \"(.*?)\"$")
	public void the_contact_s_private_address_has_moved_to(String city)
			throws Throwable {
		// check the event
		assertNotNull(addressChangedEvent);
		assertEquals(city, addressChangedEvent.getAddress().getCity());

		// check the stored result.
		AddressEntry privateAddress = null;
		List<AddressEntry> addresses = queryContactRepository
				.findAllAddressesForContact(addressChangedEvent.getContactId());
		for (AddressEntry address : addresses) {
			if (address.getAddressType() == AddressType.PRIVATE) {
				privateAddress = address;
			}
		}
		assertNotNull("No private address found", privateAddress);
		assertEquals(city, privateAddress.getCity());
	}
}
