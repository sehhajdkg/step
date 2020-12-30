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

public final class FindMeetingQuery {
  public Collection<TimeRange> query(Collection<Event> events, MeetingRequest request) {

    //If there are no events or no one is attending the event, 
    // all requests are possible if they are within the range of the whole day.
    if(request.getDuration() <= TimeRange.WHOLE_DAY.duration()){
      if(events.isEmpty() || request.getAttendees().isEmpty()) {
        return Arrays.asList(TimeRange.WHOLE_DAY);
      }
    } else {
      // Otherwise no available times.
      return Arrays.asList();
    }

    // List of existing bookedEvents for attendees in meeting request.
    List<Event> bookedEvents = new ArrayList<>();

    // Find all events that attendees in the request are already booked for.
    for(Event event : events){
      for (String attendee : request.getAttendees()){
        if(event.getAttendees().contains(attendee) && !bookedEvents.contains(event)){
          bookedEvents.add(event);
        }
      }
    }
    
    // List of potential free time ranges for the meeting request.
    List<TimeRange> potentialTime = new ArrayList<>();

    if(bookedEvents.isEmpty()){
      // attendees are not booked for any events, are available for the whole day.
      return Arrays.asList(TimeRange.WHOLE_DAY);
    } else {

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

        if(potentialTime.size() == 0) { 
          // See if there is a slot before the first booked event and if this slot.
          // is large enough for the request. 
          if(currEvent.getWhen().start() != TimeRange.START_OF_DAY) {
            // If request duration is within slot between start of day and first event.
            if((TimeRange.START_OF_DAY+request.getDuration()) <= currEvent.getWhen().start()){
              potentialTime.add(TimeRange.fromStartEnd(TimeRange.START_OF_DAY, currEvent.getWhen().start(),false));  
            } 
            potentialTime.add(TimeRange.fromStartEnd(currEvent.getWhen().end(), TimeRange.END_OF_DAY,true));       
          }
        } else {
          // remove last timerange from list (time is prevEvent.end to endOfDay) to check conflict with currEvent.
          potentialTime.remove(potentialTime.size()-1);
          
          // Check to see if prev and curr events overlap.
          // add timerange from the end of the later timerange.
          if(prevEvent.getWhen().overlaps(currEvent.getWhen())){
            int laterEnd = currEvent.getWhen().end();
            if(prevEvent.getWhen().end() > laterEnd) {
              laterEnd = prevEvent.getWhen().end();
            }
            potentialTime.add(TimeRange.fromStartEnd(laterEnd, TimeRange.END_OF_DAY,true));
          } else {
            // check gap (...) in-between curr and prev.
            // |--prev--|...|--curr--|
            long gapDuration = currEvent.getWhen().start() - prevEvent.getWhen().end();
            if(gapDuration >= request.getDuration()){
              potentialTime.add(TimeRange.fromStartEnd(prevEvent.getWhen().end(), currEvent.getWhen().start(), false));
            }
            // Check there there is a gap between the end of the event and end of day for request.
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

  }
}
