package com.group9.eventgaze.utils;

import com.group9.eventgaze.entity.Colleges;
import com.group9.eventgaze.entity.EventCategory;
import com.group9.eventgaze.entity.Events;
import com.group9.eventgaze.entity.Publishers;
import com.group9.eventgaze.entity.dto.EventResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EventMapperTest {

    private StorageUrlResolver storageUrlResolver;
    private EventMapper eventMapper;
    private static final String S3_BASE_URL = "https://eventgazeeventimages.s3.amazonaws.com/";
    private static final String CDN_BASE_URL = "https://cdn.eventgaze.com/";

    @BeforeEach
    void setUp() {
        storageUrlResolver = new StorageUrlResolver();
        eventMapper = new EventMapper(storageUrlResolver, S3_BASE_URL);
    }

    private Events createSampleEvent(String artwork) {
        Colleges college = new Colleges();
        college.setCollegeId(1L);
        college.setCollegeName("MIT");

        EventCategory category = new EventCategory();
        category.setCategoryId(2L);
        category.setCategoryName("Technology");

        Publishers publisher = new Publishers();
        publisher.setPublisherId(3L);
        publisher.setPublisherOrgName("Tech Club");
        publisher.setPublisherImage("publishers/tech.png");

        Events event = new Events();
        event.setEventId(10L);
        event.setEventName("Hackathon 2026");
        event.setEventDescription("Build cool apps");
        event.setEventDate(LocalDate.of(2026, 12, 1));
        event.setEventTags("tech,coding");
        event.setEventVenue("Grand Hall");
        event.setEventArt(artwork);
        event.setCollege(college);
        event.setEventCategory(category);
        event.setPublishers(publisher);
        return event;
    }

    @Test
    @DisplayName("Should resolve relative artwork key against configured S3 base URL")
    void testToEventResponseDTO_resolvesRelativeKey() {
        Events event = createSampleEvent("events/uuid-poster.jpg");

        EventResponseDTO dto = EventMapper.toEventResponseDTO(event);

        assertNotNull(dto);
        assertEquals(10L, dto.getEventId());
        assertEquals("Hackathon 2026", dto.getEventName());
        assertEquals("https://eventgazeeventimages.s3.amazonaws.com/events/uuid-poster.jpg", dto.getEventArt());
        assertEquals("MIT", dto.getCollege().getCollegeName());
        assertEquals("Tech Club", dto.getPublishers().getPublisherOrgName());
    }

    @Test
    @DisplayName("Should resolve relative artwork key dynamically against CDN base URL")
    void testToDTO_dynamicCdnBaseUrl() {
        EventMapper cdnMapper = new EventMapper(storageUrlResolver, CDN_BASE_URL);
        Events event = createSampleEvent("events/uuid-poster.jpg");

        EventResponseDTO dto = cdnMapper.toDTO(event);

        assertNotNull(dto);
        assertEquals("https://cdn.eventgaze.com/events/uuid-poster.jpg", dto.getEventArt());
    }

    @Test
    @DisplayName("Should preserve absolute placeholder URLs unchanged")
    void testToEventResponseDTO_preservesAbsolutePlaceholderUrl() {
        String placeholderUrl = "https://picsum.photos/400/300";
        Events event = createSampleEvent(placeholderUrl);

        EventResponseDTO dto = EventMapper.toEventResponseDTO(event);

        assertNotNull(dto);
        assertEquals(placeholderUrl, dto.getEventArt());
    }

    @Test
    @DisplayName("Should handle null artwork gracefully")
    void testToEventResponseDTO_nullArtwork() {
        Events event = createSampleEvent(null);

        EventResponseDTO dto = EventMapper.toEventResponseDTO(event);

        assertNotNull(dto);
        assertNull(dto.getEventArt());
    }

    @Test
    @DisplayName("Should handle null event input")
    void testToEventResponseDTO_nullEvent() {
        assertNull(EventMapper.toEventResponseDTO(null));
        assertNull(eventMapper.toDTO(null));
    }

    @Test
    @DisplayName("Should map list of events resolving all artwork keys")
    void testToEventResponseDTOList() {
        Events event1 = createSampleEvent("events/first.jpg");
        Events event2 = createSampleEvent("https://picsum.photos/200");
        Events event3 = createSampleEvent(null);

        List<EventResponseDTO> dtos = EventMapper.toEventResponseDTOList(List.of(event1, event2, event3));

        assertEquals(3, dtos.size());
        assertEquals("https://eventgazeeventimages.s3.amazonaws.com/events/first.jpg", dtos.get(0).getEventArt());
        assertEquals("https://picsum.photos/200", dtos.get(1).getEventArt());
        assertNull(dtos.get(2).getEventArt());
    }

    @Test
    @DisplayName("Should resolve eventArtThumb relative key and dynamic baseUrl")
    void testEventArtThumbResolution() {
        Events event = createSampleEvent("events/poster.jpg");
        event.setEventArtThumb("events/poster-thumb.jpg");

        EventResponseDTO staticDto = EventMapper.toEventResponseDTO(event);
        assertNotNull(staticDto);
        assertEquals("https://eventgazeeventimages.s3.amazonaws.com/events/poster-thumb.jpg", staticDto.getEventArtThumb());

        EventMapper cdnMapper = new EventMapper(storageUrlResolver, CDN_BASE_URL);
        EventResponseDTO dynamicDto = cdnMapper.toDTO(event);
        assertNotNull(dynamicDto);
        assertEquals("https://cdn.eventgaze.com/events/poster-thumb.jpg", dynamicDto.getEventArtThumb());
    }
}
