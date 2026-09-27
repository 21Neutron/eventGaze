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
    private static final String DEFAULT_BASE_URL = "https://eventgazeeventimages.s3.amazonaws.com/";
    private static final String CDN_BASE_URL = "https://cdn.eventgaze.com/";

    @BeforeEach
    void setUp() {
        storageUrlResolver = new StorageUrlResolver();
        eventMapper = new EventMapper(storageUrlResolver, DEFAULT_BASE_URL);
    }

    private Events createSampleEvent(String artwork) {
        Colleges college = new Colleges();
        college.setCollegeId(1L);
        college.setCollegeName("Harvard University");

        EventCategory category = new EventCategory();
        category.setCategoryId(2L);
        category.setCategoryName("Music");

        Publishers publisher = new Publishers();
        publisher.setPublisherId(3L);
        publisher.setPublisherOrgName("Arts Society");
        publisher.setPublisherImage("publishers/arts.png");

        Events event = new Events();
        event.setEventId(20L);
        event.setEventName("Symphony Night");
        event.setEventDescription("Classical Music Evening");
        event.setEventDate(LocalDate.of(2026, 12, 15));
        event.setEventTags("music,classical");
        event.setEventVenue("Symphony Hall");
        event.setEventArt(artwork);
        event.setCollege(college);
        event.setEventCategory(category);
        event.setPublishers(publisher);
        return event;
    }

    @Test
    @DisplayName("Should dynamically resolve relative artwork key in bookmark service")
    void testToEventResponseDTO_resolvesRelativeKey() {
        Events event = createSampleEvent("events/symphony.jpg");

        EventResponseDTO dto = EventMapper.toEventResponseDTO(event);

        assertNotNull(dto);
        assertEquals(20L, dto.getEventId());
        assertEquals("Symphony Night", dto.getEventName());
        assertEquals("https://eventgazeeventimages.s3.amazonaws.com/events/symphony.jpg", dto.getEventArt());
        assertEquals("Harvard University", dto.getCollege().getCollegeName());
    }

    @Test
    @DisplayName("Should dynamically resolve relative artwork key with custom CDN base URL")
    void testToDTO_dynamicCdnBaseUrl() {
        EventMapper cdnMapper = new EventMapper(storageUrlResolver, CDN_BASE_URL);
        Events event = createSampleEvent("events/symphony.jpg");

        EventResponseDTO dto = cdnMapper.toDTO(event);

        assertNotNull(dto);
        assertEquals("https://cdn.eventgaze.com/events/symphony.jpg", dto.getEventArt());
    }

    @Test
    @DisplayName("Should preserve absolute placeholder URLs unchanged")
    void testToEventResponseDTO_preservesAbsolutePlaceholderUrl() {
        String placeholderUrl = "https://picsum.photos/300/200";
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
    @DisplayName("Should map list of bookmarked events resolving all artwork keys")
    void testToEventResponseDTOList() {
        Events event1 = createSampleEvent("events/one.jpg");
        Events event2 = createSampleEvent("https://picsum.photos/100");
        Events event3 = createSampleEvent(null);

        List<EventResponseDTO> dtos = EventMapper.toEventResponseDTOList(List.of(event1, event2, event3));

        assertEquals(3, dtos.size());
        assertEquals("https://eventgazeeventimages.s3.amazonaws.com/events/one.jpg", dtos.get(0).getEventArt());
        assertEquals("https://picsum.photos/100", dtos.get(1).getEventArt());
        assertNull(dtos.get(2).getEventArt());
    }

    @Test
    @DisplayName("Should resolve eventArtThumb relative key and dynamic baseUrl")
    void testEventArtThumbResolution() {
        Events event = createSampleEvent("events/symphony.jpg");
        event.setEventArtThumb("events/symphony-thumb.jpg");

        EventResponseDTO staticDto = EventMapper.toEventResponseDTO(event);
        assertNotNull(staticDto);
        assertEquals("https://eventgazeeventimages.s3.amazonaws.com/events/symphony-thumb.jpg", staticDto.getEventArtThumb());

        EventMapper cdnMapper = new EventMapper(storageUrlResolver, CDN_BASE_URL);
        EventResponseDTO dynamicDto = cdnMapper.toDTO(event);
        assertNotNull(dynamicDto);
        assertEquals("https://cdn.eventgaze.com/events/symphony-thumb.jpg", dynamicDto.getEventArtThumb());
    }
}
