Feature: Contact Maintenance
 
Scenario: Creating a new contact
Given an empty address book
When I add a contact with the name "John Doe"
Then the contact "John Doe" is added to the address book

Scenario: Changing the name of a contact
Given the contact "John Doe"
When I change the name to "John T. Doe"
Then the name is changed to "John T. Doe"

Scenario: Deleting a contact
Given the contact "John Doe"
When I delete this contact
Then the contact "John Doe" can no longer be found in the address book

Scenario: Duplicate contact
Given the contact "John Doe"
When I add a contact with the same name
Then I receive a message that this contact already exists

Scenario: Adding addresses
Given the contact "Allan Doe"
When I add the following addresses:
 | addressType | streetAndNumber | zipCode | city |
 | WORK | Church road 49 | 12312 | Oxford |
 | PRIVATE | Maleo 15 | 455354 | Cambridge |
 | VACATION |Av. Tomas Cabreira Praia da Rocha |8500-802 | Portimão |
Then i can find the contact if i search for all contacts in "Cambridge"
And i can find the contact if i search for all contacts in "Oxford"
And i can find the contact if i search for all contacts in "Portimão" 

Scenario: Removing an address
Given the contact "Jan Janssen"
When I add the following addresses:
 | addressType | streetAndNumber | zipCode | city |
 | WORK | Church road 41 | 12314 | Oxford |
 | PRIVATE | Main street 1 | 13451 | London |
 | VACATION |Av. Tomas Cabreira Praia da Rocha |8500-802 | Portimão |
And delete the "VACATION" address
Then the contact has a "WORK" and "PRIVATE" address left

Scenario: Changing an address
Given the contact "Pieter Klaassen"
When I add the following addresses:
 | addressType | streetAndNumber | zipCode | city |
 | PRIVATE | Times Square 1 | 53443 | New York |
And I add the following address: 
 | addressType | streetAndNumber | zipCode | city |
 | PRIVATE | High way 1 | 43532 | San Fransisco |
Then the contact's private address has moved to "San Fransisco" 

 
