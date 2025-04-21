# EventGaze - Event Management System

EventGaze is a comprehensive event management platform designed for college students and event publishers. It allows students to discover, bookmark, and attend events while enabling publishers to create and manage their events.

## Database Schema
![Database Schema](database_schema.png)

## Project Overview

EventGaze is a Spring Boot application that provides a RESTful API for event management. The system has the following key features:

- **User Authentication & Authorization**: Secure JWT-based authentication system with different user roles (student and publisher).
- **Event Management**: Create, read, update, and delete events.
- **Event Categories**: Events are categorized (Hackathons, Sports, Music, Dance, etc.).
- **College/University Integration**: Events can be associated with specific colleges.
- **Student Bookmarks**: Students can bookmark events they're interested in.
- **Stateless Authentication**: Uses JWT tokens for secure, stateless authentication.

## Tech Stack

- **Backend**: Java 17, Spring Boot 3.3
- **Database**: MySQL
- **Security**: Spring Security with stateless JWT authentication
- **File Storage**: AWS S3 for storing event artwork
- **Build Tool**: Maven

## API Endpoints

### Authentication

- `POST /eventgaze/auth/student` - Register a new student account
- `POST /eventgaze/auth/publisher` - Register a new publisher account
- `POST /eventgaze/auth/login` - Login for both student and publisher accounts

### Events

- `GET /eventgaze/events/getAll` - Get all events
- `POST /eventgaze/events/create` - Create a new event
- `GET /eventgaze/events/eventId/id/{id}` - Get event by ID
- `GET /eventgaze/events/category/id/{categoryId}` - Get events by category
- `GET /eventgaze/events/college/id/{collegeId}` - Get events by college
- `DELETE /eventgaze/events/id/{id}` - Delete event by ID
- `PUT /eventgaze/events/eventEdit/id/{id}` - Update event by ID

### Colleges

- `GET /eventgaze/collegeList/getAll` - Get a list of all colleges
- `GET /eventgaze/collegeList/id{myId}` - Get a specific college by ID

### Event Categories

- `GET /eventgaze/category/getAll` - Get a list of all event categories

## Project Structure

The project follows a standard Spring Boot application structure:

- `controller/` - REST API controllers
- `service/` - Business logic services
- `repository/` - Data access interfaces
- `entity/` - Data models/entities
- `config/` - Application configurations
- `utils/` - Utility classes

## Database Models

The database schema consists of the following main entities:

1. **UserCredential** - Stores user authentication information
2. **Publisher** - Organizations/entities that create events
3. **Student** - Users who can view and bookmark events
4. **Events** - The main entity representing events
5. **StudentBookmarks** - Junction table for student event bookmarks
6. **College_list** - List of colleges/universities
7. **EventCategory** - Categories for events (Hackathons, Sports, etc.)

## Setup and Installation

### Prerequisites

- Java 17 or higher
- MySQL 8.0 or higher
- Maven
- AWS Account (for S3 storage)

### Configuration

1. Clone the repository
2. Configure the database in `application.properties`
3. Set up AWS credentials for S3 access
4. Configure JWT secret key

```properties
# Database configuration
spring.datasource.url=jdbc:mysql://localhost:3306/eventgaze
spring.datasource.username=your_username
spring.datasource.password=your_password

# AWS Configuration
AWS.ACCESS.KEY.ID=your_access_key
AWS.SECRET.ACCESS.KEY=your_secret_key
AWS.REGION=your_aws_region

# JWT Configuration
JWT.SECRET.KEY=your_jwt_secret_key
```

### Building and Running

```bash
# Build the project
mvn clean install

# Run the application
mvn spring-boot:run
```

## Security

The application implements:
- Stateless JWT-based authentication
- Password encryption
- Role-based access control
- Token validation for every request

## File Storage

Event artwork and images are stored in AWS S3, configured through the `S3Config` class and managed by the `S3Service`.

## Development Team

Developed by Group9 at ADGIPS. 