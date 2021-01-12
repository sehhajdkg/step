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
  /**
   * The basic functionality of query is that if one or more time slots exists 
   * so that both mandatory and optional attendees can attend, return those time slots. 
   * Otherwise, return the time slots that fit just the mandatory attendees.
   * @param events
   * @param request
   * @return
   */
  public Collection<TimeRange> query(Collection<Event> events, MeetingRequest request) {

    // No possible meeting if request is longer than a day.
    if (request.getDuration() > TimeRange.WHOLE_DAY.duration()){
      return Arrays.asList();
    }
    // No events or attendees means meeting request can be anytime in the day.
    if(events.isEmpty() || (request.getAttendees().isEmpty() && request.getOptionalAttendees().isEmpty()) ) {
        return Arrays.asList(TimeRange.WHOLE_DAY);
    } 

    List<Event> mandatoryBookedEvents = findBookedEvents(events, request.getAttendees());
   
    List<Event> optionalBookedEvents = findBookedEvents(events, request.getOptionalAttendees());

    List<Event> allBookedEvents = getAllUniqueEvents(mandatoryBookedEvents, optionalBookedEvents);
    allBookedEvents = sortEventsByStartTime(allBookedEvents);

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
    Collection<TimeRange> potentialTime = new ArrayList<>();

    // No bookedEvents, request can be anytime.  
    if(bookedEvents.isEmpty()){
      return Arrays.asList(TimeRange.WHOLE_DAY);
    }

    // If only one event - consider only timerange before and after the event.
    if(bookedEvents.size() == 1) {
      if((TimeRange.START_OF_DAY+request.getDuration()) < bookedEvents.get(0).getWhen().start()){
        potentialTime.add(TimeRange.fromStartEnd(TimeRange.START_OF_DAY, bookedEvents.get(0).getWhen().start(),false));  
      }
      if(bookedEvents.get(0).getWhen().end()+request.getDuration() <= TimeRange.END_OF_DAY){
        potentialTime.add(TimeRange.fromStartEnd(bookedEvents.get(0).getWhen().end(), TimeRange.END_OF_DAY,true));  
      }
      return potentialTime;
    }  

    // More than one event - check if timeslot before first event can fit request
    if((TimeRange.START_OF_DAY+request.getDuration()) < bookedEvents.get(0).getWhen().start()){
      potentialTime.add(TimeRange.fromStartEnd(TimeRange.START_OF_DAY, bookedEvents.get(0).getWhen().start(),false));  
    }

    Event prevEvent = bookedEvents.get(0); 
    Event currEvent = bookedEvents.get(0);
    for(int i = 1; i < bookedEvents.size(); i++){ 
      prevEvent = currEvent; 
      currEvent = bookedEvents.get(i); 
        // Check to see if prev and curr events overlap.
        // add timerange from the end of the later timerange.
        if(prevEvent.getWhen().overlaps(currEvent.getWhen())){
          int laterEnd = currEvent.getWhen().end();
          if(prevEvent.getWhen().end() > laterEnd) {
            laterEnd = prevEvent.getWhen().end();
          }

          if(laterEnd+request.getDuration() <= TimeRange.END_OF_DAY){
            potentialTime.add(TimeRange.fromStartEnd(laterEnd, TimeRange.END_OF_DAY,true));
          } else {
            // no further timeslots can fit the request
            break;
          }
        } else {
          // check GAP (...) in-between curr and prev.
          // |--prev--|...|--curr--|
          long gapDuration = currEvent.getWhen().start() - prevEvent.getWhen().end();
          if(gapDuration >= request.getDuration()){
            potentialTime.add(TimeRange.fromStartEnd(prevEvent.getWhen().end(), currEvent.getWhen().start(), false));
          }
          // Last event in list then check there there is a gap between 
          // the end of the event and end of day large enough for request.
          if (currEvent == bookedEvents.get(bookedEvents.size()-1)){
            if(bookedEvents.get(bookedEvents.size()-1).getWhen().end()+request.getDuration() <= TimeRange.END_OF_DAY){
              potentialTime.add(TimeRange.fromStartEnd(bookedEvents.get(bookedEvents.size()-1).getWhen().end(), TimeRange.END_OF_DAY,true));  
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
   * @param events - list of events to be sorted.
   * @return sorted list of events by ascending start time.
   */
  public List<Event> sortEventsByStartTime(List<Event> events) {

    Collections.sort(events, (event1, event2) -> Long.compare(event1.getWhen().start(), event2.getWhen().start()));
    return events;
  }  
}
