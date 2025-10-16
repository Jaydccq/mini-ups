package com.miniups.shortlink.e2e;

import com.miniups.shortlink.dto.ShortLinkCreateRequest;
import com.miniups.shortlink.dto.ShortLinkCreateResponse;
import com.miniups.shortlink.model.ShortLinkRecord;
import com.miniups.shortlink.repository.ShortLinkRepository;
import com.miniups.shortlink.service.ShortLinkService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Workflow-based end-to-end tests for the ShortLink system.
 * Tests realistic business scenarios and user workflows.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Transactional
class ShortLinkWorkflowTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("shortlink_workflow_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    private ShortLinkService shortLinkService;

    @Autowired
    private ShortLinkRepository shortLinkRepository;

    private MockHttpServletRequest mockRequest;

    @BeforeEach
    void setUp() {
        mockRequest = new MockHttpServletRequest();
        mockRequest.addHeader("X-Forwarded-For", "192.168.1.100");
        mockRequest.addHeader("User-Agent", "Mozilla/5.0 Test Browser");
    }

    @Test
    void businessUser_CreatesAndManagesMarketingCampaign() {
        // Scenario: Marketing team creates short links for campaign
        Long marketingUserId = 1001L;
        String campaignName = "SpringSale2024";

        // Step 1: Create multiple campaign links
        String[] campaignUrls = {
                "https://shop.example.com/spring-sale/electronics?utm_campaign=" + campaignName,
                "https://shop.example.com/spring-sale/clothing?utm_campaign=" + campaignName,
                "https://shop.example.com/spring-sale/home?utm_campaign=" + campaignName
        };

        String[] descriptions = {
                "Electronics Spring Sale",
                "Clothing Spring Sale",
                "Home & Garden Spring Sale"
        };

        // Create all campaign links
        for (int i = 0; i < campaignUrls.length; i++) {
            ShortLinkCreateRequest request = new ShortLinkCreateRequest();
            request.setOriginalUrl(campaignUrls[i]);
            request.setDescription(descriptions[i]);
            request.setExpirationDays(30); // Campaign runs for 30 days

            ShortLinkCreateResponse response = shortLinkService.createShortLink(marketingUserId, request);

            assertThat(response.getShortCode()).isNotNull();
            assertThat(response.getShortUrl()).contains(response.getShortCode());
        }

        // Step 2: Verify all links are stored with correct metadata
        List<ShortLinkRecord> userLinks = shortLinkRepository.findByUserId(marketingUserId);
        assertThat(userLinks).hasSize(3);

        for (ShortLinkRecord link : userLinks) {
            assertThat(link.getUserId()).isEqualTo(marketingUserId);
            assertThat(link.getOriginalUrl()).contains(campaignName);
            assertThat(link.getDescription()).contains("Spring Sale");
            assertThat(link.isActive()).isTrue();
            assertThat(link.getExpirationAt()).isAfter(LocalDateTime.now().plusDays(25));
        }

        // Step 3: Simulate customer clicks on campaign links
        for (ShortLinkRecord link : userLinks) {
            // Simulate multiple clicks from different sources
            for (int click = 0; click < 5; click++) {
                String redirectUrl = shortLinkService.resolveRedirect(link.getShortCode(), mockRequest);
                assertThat(redirectUrl).isEqualTo(link.getOriginalUrl());
            }
        }

        // Step 4: Verify analytics data
        for (ShortLinkRecord link : userLinks) {
            Optional<ShortLinkRecord> updated = shortLinkRepository.findByShortCode(link.getShortCode());
            assertThat(updated).isPresent();
            assertThat(updated.get().getAccessCount()).isEqualTo(5);
            assertThat(updated.get().getLastAccessAt()).isAfter(link.getCreatedAt());
        }
    }

    @Test
    void contentCreator_SharesSocialMediaLinks() {
        // Scenario: Content creator shares multiple platform links
        Long creatorUserId = 2001L;

        String[] socialPlatforms = {
                "https://youtube.com/watch?v=abc123&list=playlist1",
                "https://instagram.com/p/ABC123XYZ/",
                "https://twitter.com/creator/status/1234567890",
                "https://tiktok.com/@creator/video/9876543210"
        };

        String[] platformNames = {"YouTube", "Instagram", "Twitter", "TikTok"};

        // Step 1: Create short links for all platforms
        for (int i = 0; i < socialPlatforms.length; i++) {
            ShortLinkCreateRequest request = new ShortLinkCreateRequest();
            request.setOriginalUrl(socialPlatforms[i]);
            request.setDescription(platformNames[i] + " Content Link");
            request.setExpirationDays(90); // Long-term content

            ShortLinkCreateResponse response = shortLinkService.createShortLink(creatorUserId, request);
            assertThat(response.getShortCode()).isNotNull();
        }

        // Step 2: Simulate viral content - lots of clicks
        List<ShortLinkRecord> creatorLinks = shortLinkRepository.findByUserId(creatorUserId);
        assertThat(creatorLinks).hasSize(4);

        // Simulate different popularity levels per platform
        int[] clickCounts = {150, 89, 203, 45}; // YouTube, Instagram, Twitter, TikTok

        for (int i = 0; i < creatorLinks.size(); i++) {
            ShortLinkRecord link = creatorLinks.get(i);

            // Simulate clicks
            for (int click = 0; click < clickCounts[i]; click++) {
                String redirectUrl = shortLinkService.resolveRedirect(link.getShortCode(), mockRequest);
                assertThat(redirectUrl).isEqualTo(link.getOriginalUrl());
            }
        }

        // Step 3: Verify analytics reflect viral nature
        for (int i = 0; i < creatorLinks.size(); i++) {
            ShortLinkRecord link = creatorLinks.get(i);
            Optional<ShortLinkRecord> updated = shortLinkRepository.findByShortCode(link.getShortCode());
            assertThat(updated).isPresent();
            assertThat(updated.get().getAccessCount()).isEqualTo(clickCounts[i]);
        }
    }

    @Test
    void eventOrganizer_ManagesEventRegistrations() {
        // Scenario: Event organizer creates links for different ticket types
        Long organizerUserId = 3001L;
        String eventName = "TechConf2024";

        String[] ticketTypes = {
                "https://events.example.com/techconf2024/register?type=early-bird",
                "https://events.example.com/techconf2024/register?type=regular",
                "https://events.example.com/techconf2024/register?type=vip",
                "https://events.example.com/techconf2024/register?type=student"
        };

        String[] ticketNames = {"Early Bird", "Regular", "VIP", "Student"};

        // Step 1: Create registration links
        for (int i = 0; i < ticketTypes.length; i++) {
            ShortLinkCreateRequest request = new ShortLinkCreateRequest();
            request.setOriginalUrl(ticketTypes[i]);
            request.setDescription(eventName + " - " + ticketNames[i] + " Registration");
            request.setExpirationDays(60); // Event in 2 months

            ShortLinkCreateResponse response = shortLinkService.createShortLink(organizerUserId, request);
            assertThat(response.getShortCode()).isNotNull();
        }

        // Step 2: Simulate early promotion phase (early bird gets most traffic)
        List<ShortLinkRecord> eventLinks = shortLinkRepository.findByUserId(organizerUserId);
        assertThat(eventLinks).hasSize(4);

        // Early promotion: mostly early bird registrations
        ShortLinkRecord earlyBirdLink = eventLinks.stream()
                .filter(link -> link.getOriginalUrl().contains("early-bird"))
                .findFirst()
                .orElseThrow();

        // Simulate 50 early bird registrations
        IntStream.range(0, 50).forEach(i -> {
            String url = shortLinkService.resolveRedirect(earlyBirdLink.getShortCode(), mockRequest);
            assertThat(url).contains("early-bird");
        });

        // Step 3: Simulate regular promotion phase
        ShortLinkRecord regularLink = eventLinks.stream()
                .filter(link -> link.getOriginalUrl().contains("type=regular"))
                .findFirst()
                .orElseThrow();

        // Simulate 30 regular registrations
        IntStream.range(0, 30).forEach(i -> {
            String url = shortLinkService.resolveRedirect(regularLink.getShortCode(), mockRequest);
            assertThat(url).contains("type=regular");
        });

        // Step 4: Verify registration analytics
        Optional<ShortLinkRecord> updatedEarlyBird = shortLinkRepository.findByShortCode(earlyBirdLink.getShortCode());
        Optional<ShortLinkRecord> updatedRegular = shortLinkRepository.findByShortCode(regularLink.getShortCode());

        assertThat(updatedEarlyBird).isPresent();
        assertThat(updatedEarlyBird.get().getAccessCount()).isEqualTo(50);

        assertThat(updatedRegular).isPresent();
        assertThat(updatedRegular.get().getAccessCount()).isEqualTo(30);
    }

    @Test
    void ecommerceStore_TracksProductPerformance() {
        // Scenario: E-commerce store tracks product link performance
        Long storeUserId = 4001L;

        String[] productCategories = {"electronics", "clothing", "books", "home"};
        String[] productIds = {"PROD001", "PROD002", "PROD003", "PROD004"};

        // Step 1: Create product links for different campaigns
        for (int i = 0; i < productCategories.length; i++) {
            String productUrl = String.format(
                    "https://store.example.com/products/%s/%s?utm_source=shortlink&utm_medium=social",
                    productCategories[i], productIds[i]
            );

            ShortLinkCreateRequest request = new ShortLinkCreateRequest();
            request.setOriginalUrl(productUrl);
            request.setDescription("Product: " + productIds[i] + " (" + productCategories[i] + ")");
            request.setExpirationDays(365); // Products available year-round

            ShortLinkCreateResponse response = shortLinkService.createShortLink(storeUserId, request);
            assertThat(response.getShortCode()).isNotNull();
        }

        // Step 2: Simulate different product popularity
        List<ShortLinkRecord> productLinks = shortLinkRepository.findByUserId(storeUserId);
        assertThat(productLinks).hasSize(4);

        // Simulate realistic e-commerce traffic patterns
        int[] productViews = {120, 45, 78, 89}; // electronics most popular

        for (int i = 0; i < productLinks.size(); i++) {
            ShortLinkRecord link = productLinks.get(i);

            for (int view = 0; view < productViews[i]; view++) {
                String redirectUrl = shortLinkService.resolveRedirect(link.getShortCode(), mockRequest);
                assertThat(redirectUrl).contains(productIds[i]);
            }
        }

        // Step 3: Analyze product performance
        for (int i = 0; i < productLinks.size(); i++) {
            ShortLinkRecord link = productLinks.get(i);
            Optional<ShortLinkRecord> updated = shortLinkRepository.findByShortCode(link.getShortCode());
            assertThat(updated).isPresent();
            assertThat(updated.get().getAccessCount()).isEqualTo(productViews[i]);
        }

        // Step 4: Verify top performing products
        List<ShortLinkRecord> allUpdated = shortLinkRepository.findByUserId(storeUserId);
        ShortLinkRecord topPerformer = allUpdated.stream()
                .max((a, b) -> Integer.compare(a.getAccessCount(), b.getAccessCount()))
                .orElseThrow();

        assertThat(topPerformer.getOriginalUrl()).contains("electronics");
        assertThat(topPerformer.getAccessCount()).isEqualTo(120);
    }

    @Test
    void newsOrganization_ManagesBreakingNewsLinks() {
        // Scenario: News organization needs to quickly share breaking news
        Long newsUserId = 5001L;

        // Step 1: Create breaking news link
        ShortLinkCreateRequest breakingNews = new ShortLinkCreateRequest();
        breakingNews.setOriginalUrl("https://news.example.com/breaking/major-tech-announcement-2024");
        breakingNews.setDescription("Breaking: Major Tech Announcement");
        breakingNews.setExpirationDays(7); // News has short lifespan

        ShortLinkCreateResponse newsResponse = shortLinkService.createShortLink(newsUserId, breakingNews);
        assertThat(newsResponse.getShortCode()).isNotNull();

        String newsCode = newsResponse.getShortCode();

        // Step 2: Simulate viral news sharing (high traffic spike)
        int viralClicks = 500;
        for (int i = 0; i < viralClicks; i++) {
            String redirectUrl = shortLinkService.resolveRedirect(newsCode, mockRequest);
            assertThat(redirectUrl).contains("major-tech-announcement");
        }

        // Step 3: Verify high-traffic handling
        Optional<ShortLinkRecord> viralNews = shortLinkRepository.findByShortCode(newsCode);
        assertThat(viralNews).isPresent();
        assertThat(viralNews.get().getAccessCount()).isEqualTo(viralClicks);

        // Step 4: Create follow-up stories
        String[] followUpStories = {
                "https://news.example.com/analysis/tech-announcement-impact",
                "https://news.example.com/opinion/what-this-means-for-industry",
                "https://news.example.com/live/tech-announcement-reactions"
        };

        for (String storyUrl : followUpStories) {
            ShortLinkCreateRequest followUp = new ShortLinkCreateRequest();
            followUp.setOriginalUrl(storyUrl);
            followUp.setDescription("Follow-up: Tech Announcement Coverage");
            followUp.setExpirationDays(3);

            ShortLinkCreateResponse followUpResponse = shortLinkService.createShortLink(newsUserId, followUp);
            assertThat(followUpResponse.getShortCode()).isNotNull();
        }

        // Verify all news links are tracked
        List<ShortLinkRecord> allNewsLinks = shortLinkRepository.findByUserId(newsUserId);
        assertThat(allNewsLinks).hasSize(4); // 1 breaking + 3 follow-ups
    }

    @Test
    void personalUser_SharesVariousContent() {
        // Scenario: Regular user shares personal content and links
        Long personalUserId = 6001L;

        String[] personalLinks = {
                "https://photos.example.com/vacation-album-2024",
                "https://github.com/user/awesome-project",
                "https://blog.example.com/my-thoughts-on-tech",
                "https://recipe.example.com/grandmas-secret-cookies"
        };

        String[] descriptions = {
                "My Vacation Photos",
                "Check out my GitHub project",
                "My latest blog post",
                "Family cookie recipe"
        };

        // Step 1: Create personal sharing links
        for (int i = 0; i < personalLinks.length; i++) {
            ShortLinkCreateRequest request = new ShortLinkCreateRequest();
            request.setOriginalUrl(personalLinks[i]);
            request.setDescription(descriptions[i]);
            request.setExpirationDays(180); // Personal content, longer term

            ShortLinkCreateResponse response = shortLinkService.createShortLink(personalUserId, request);
            assertThat(response.getShortCode()).isNotNull();
        }

        // Step 2: Simulate personal sharing (modest traffic)
        List<ShortLinkRecord> personalShares = shortLinkRepository.findByUserId(personalUserId);
        assertThat(personalShares).hasSize(4);

        // Simulate realistic personal sharing traffic
        int[] shareClicks = {15, 8, 23, 12}; // Modest personal network traffic

        for (int i = 0; i < personalShares.size(); i++) {
            ShortLinkRecord link = personalShares.get(i);

            for (int click = 0; click < shareClicks[i]; click++) {
                String redirectUrl = shortLinkService.resolveRedirect(link.getShortCode(), mockRequest);
                assertThat(redirectUrl).isEqualTo(link.getOriginalUrl());
            }
        }

        // Step 3: Verify personal sharing analytics
        for (int i = 0; i < personalShares.size(); i++) {
            Optional<ShortLinkRecord> updated = shortLinkRepository.findByShortCode(personalShares.get(i).getShortCode());
            assertThat(updated).isPresent();
            assertThat(updated.get().getAccessCount()).isEqualTo(shareClicks[i]);
        }
    }
}