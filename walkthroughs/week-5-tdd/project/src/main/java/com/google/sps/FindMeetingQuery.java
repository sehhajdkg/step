// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.sps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Collections;

public final class FindMeetingQuery {
  public Collection<TimeRange> query(Collection<Event> events, MeetingRequest request) {

    //If there are no events or no one is attending the event, 
    // all requests are possible if they are within the range of the whole day.

    // The basic functionality of optional attendees is that if one or more time slots exists 
    // so that both mandatory and optional attendees can attend, return those time slots. 
    // Otherwise, return the time slots that fit just the mandatory attendees.

    // Find all events that attendees in the meeting request are already booked for.

    // Mandatory attendees.
    List<Event> mandatoryBookedEvents = findBookedEvents(events, request.getAttendees());
   
    // Optional attendes.
    List<Event> optionalBookedEvents = findBookedEvents(events, request.getOptionalAttendees());

    // All attendees - Mandatory + Optional.
    List<Event> allBookedEvents = getAllUniqueEvents(mandatoryBookedEvents, optionalBookedEvents);
    allBookedEvents = sortEventsByStartTime(allBookedEvents);

    // No possible meeting if request is longer than a day.
    if (request.getDuration() > TimeRange.WHOLE_DAY.duration()){
      return Arrays.asList();
    }
    // No events/no attendess means meeting can be anytime.
    if(events.isEmpty() || (mandatoryBookedEvents.isEmpty() && optionalBookedEvents.isEmpty())) {
        return Arrays.asList(TimeRange.WHOLE_DAY);
    } 

    // If mandatory attendees are available all day with no booked events.
    // Try and find available times including optional event restrictions and return unless.
    // no time that works with optional attendess works just return mandatory availabilities (All day).
    if(mandatoryBookedEvents.isEmpty()){
      Collection<TimeRange> allAvailable = findPotentialTimes(optionalBookedEvents, request);
      
      if(allAvailable.isEmpty()){
        return Arrays.asList(TimeRange.WHOLE_DAY);
      }
      return allAvailable;
    }

    // Try finding time with all attendees, if none can be found try finding time with
    // only mandatory attendees.
    Collection<TimeRange> times = findPotentialTimes(allBookedEvents, request);
    if(times.isEmpty()){
      return findPotentialTimes(mandatoryBookedEvents, request);
    }
    return times;
  }

  /**
   * Finds all the events that attendees in the request are booked for.
   * @param events - all possible events.
   * @param attendees - from the meeting request.
   * @return events that request attendees are booked for.
   */
  public List<Event> findBookedEvents(Collection<Event> events, Collection<String> attendees) {
    // List of existing bookedEvents for attendees in meeting request.
    List<Event> bookedEvents = new ArrayList<>();

    // Find all events that attendees in the request are already booked for.
    for(Event event : events){
      for (String attendee : attendees){
        if(event.getAttendees().contains(attendee) && !bookedEvents.contains(event)){
          bookedEvents.add(event);
        }
      }
    }
    return bookedEvents;
  } 
  /**
   * Finds all potential availabilities in between bookedEvents.
   * @param bookedEvents - timerange already booked out/unavailable.
   * @param request - meetin request to add in a potential new timerange.
   * @return list of timeranges for availability.
   */
  public Collection<TimeRange> findPotentialTimes(List<Event> bookedEvents, MeetingRequest request){
    // List of potential free timeranges for the meeting request.
    List<TimeRange> potentialTime = new ArrayList<>();
    
    Event prevEvent = bookedEvents.get(0); 
    Event currEvent = bookedEvents.get(0);
    for(int i = 0; i < bookedEvents.size(); i++){ 
      prevEvent = currEvent; 
      currEvent = bookedEvents.get(i); 

      // If only one event - consider only timerange before and after the event.
      if(bookedEvents.size() == 1) {
        potentialTime.add(TimeRange.fromStartEnd(TimeRange.START_OF_DAY, currEvent.getWhen().start(), false));
        potentialTime.add(TimeRange.fromStartEnd(currEvent.getWhen().end(), TimeRange.END_OF_DAY, true));
        break;
      }

      // No times found yet.
      if(potentialTime.size() == 0) { 
        // See if there is a slot before the first booked event and if this slot.
        // is large enough for the request. 
        if(currEvent.getWhen().start() != TimeRange.START_OF_DAY) {
          // If request duration is within slot between start of day and first event (curr).
          if((TimeRange.START_OF_DAY+request.getDuration()) < currEvent.getWhen().start()){
            potentialTime.add(TimeRange.fromStartEnd(TimeRange.START_OF_DAY, currEvent.getWhen().start(),false));  
          }
        }
        // If meeting request can fit in between end of event and end of day.
        if((currEvent.getWhen().end()+request.getDuration()) <= TimeRange.END_OF_DAY){
            potentialTime.add(TimeRange.fromStartEnd(currEvent.getWhen().end(), TimeRange.END_OF_DAY,true));      
        }
      } else {
        // Time slots have been found.
        // Remove the last timerange from list (time is prevEvent.end to endOfDay) to check conflict with currEvent.
        potentialTime.remove(potentialTime.size()-1);
        
        // Check to see if prev and curr events OVERLAP.
        // Add timerange from the end of the later timerange.
        if(prevEvent.getWhen().overlaps(currEvent.getWhen())){
          int laterEnd = currEvent.getWhen().end();
          if(prevEvent.getWhen().end() > laterEnd) {
            laterEnd = prevEvent.getWhen().end();
          }
          potentialTime.add(TimeRange.fromStartEnd(laterEnd, TimeRange.END_OF_DAY,true));
        } else {
          // check GAP (...) in-between curr and prev.
          // |--prev--|...|--curr--|
          long gapDuration = currEvent.getWhen().start() - prevEvent.getWhen().end();
          if(gapDuration >= request.getDuration()){
            potentialTime.add(TimeRange.fromStartEnd(prevEvent.getWhen().end(), currEvent.getWhen().start(), false));
          }
          // Check there there is a gap between the end of the event and end of day large enough for request.
          if(currEvent.getWhen().end() != TimeRange.END_OF_DAY){
            if((currEvent.getWhen().end()+request.getDuration()) <= TimeRange.END_OF_DAY){
              potentialTime.add(TimeRange.fromStartEnd(currEvent.getWhen().end(), TimeRange.END_OF_DAY,true));  
            } 
          }
        }
      }
    }
    return potentialTime;
  }
  /**
   * Finds all events that mandatory and optional attendees are booked for - removes duplicates.
   * @param mandatory - events that mandatory attendees in the request have to attend.
   * @param optional - events that optional attendees in the request have to attend.
   * @return list of events.
   */
  public List<Event> getAllUniqueEvents(List<Event> mandatoryEvents, List<Event> optionalEvents) {

    List<Event> allEvents = new ArrayList<>(mandatoryEvents);

    for(Event optionalEvent : optionalEvents){
      if(!allEvents.contains(optionalEvent)){
        allEvents.add(optionalEvent);
      }
    }
    return allEvents;
  } 
  /**
   * Basic Bubble sorting of event list so they are in ascending order by start time.
   * @param events - list of events to be compared by start time.
   * @return sorted list of events.
   */
  public List<Event> sortEventsByStartTime(List<Event> events) {


    events.sort(c);

    Comparator<TimeRange> ORDER_BY_START = TimeRange.ORDER_BY_START;
    Collections.sort(events, ORDER_BY_START);

  //   for(int i = 0; i < events.size()-1; i++) {
  //     for(int j = 0; j < events.size()-i-1; j++){
  //       if(events.get(j).getWhen().start() > events.get(j+1).getWhen().start()){
  //         Event temp = events.get(j+1);
  //         events.set(j+1, events.get(j));
  //         events.set(j, temp);
  //       }
  //     }
  //   }
  //   return events;
  // } 
}
